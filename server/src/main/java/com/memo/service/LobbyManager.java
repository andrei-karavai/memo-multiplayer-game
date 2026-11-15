package com.memo.service;


import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.memo.dto.CreateMatchRequest;
import com.memo.dto.MatchLobbyResponse;
import com.memo.model.GameMatch;
import com.memo.model.LobbyRoom;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;


/**
 * Centralized thread-safe service managing both pre-game waiting rooms ({@link LobbyRoom})
 * and active in-memory game sessions ({@link GameMatch}).
 * Coordinates player seating, auto-start countdown scheduling, deck generation,
 * and real-time STOMP lobby state broadcasts.
 */
@Service
public class LobbyManager {

    // Tracks active matches available on server.
    private final Map<String, GameMatch> activeMatches;

    // Tracks active lobby rooms available on server.
    private final Map<String, LobbyRoom> waitingRooms;

    // Shared timer engine used to schedule the auto-start countdown.
    private final ScheduledExecutorService scheduler;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Initializes the in-memory lobby and match registries, the background countdown scheduler,
     * and the STOMP messaging template used for real-time lobby broadcasts.
     *
     * @param messagingTemplate Spring messaging template for pushing updates to {@code /topic/lobby/{matchId}}.
     */
    public LobbyManager(SimpMessagingTemplate  messagingTemplate) {

        this.waitingRooms = new ConcurrentHashMap<>();
        this.activeMatches = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.messagingTemplate = messagingTemplate;

    }

    /**
     * Validates match creation parameters, creates a new waiting room with a unique 6-character code,
     * and seats the creating host as the first player.
     *
     * @param request Incoming lobby configuration containing host ID, pair count (16–24), and max players (2–4).
     * @return A {@link MatchLobbyResponse} snapshot representing the newly created waiting room.
     * @throws IllegalArgumentException if the request is null or any parameter falls outside allowed bounds.
     */
    public MatchLobbyResponse createLobby(CreateMatchRequest request) {
        if(request == null ||
           request.getHostPlayerId() == null ||
           request.getHostPlayerId().isBlank() ||
           request.getPairCount() < 16 ||
           request.getPairCount() > 24 ||
           request.getMaxPlayers() < 2 ||
           request.getMaxPlayers() > 4) {
            throw new IllegalArgumentException("Invalid request.");
        }

        String matchId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LobbyRoom room = new LobbyRoom(matchId, request.getHostPlayerId().trim(), request.getPairCount(), request.getMaxPlayers(), scheduler, () -> startMatch(matchId));
        waitingRooms.put(matchId, room);

        return room.toResponse();
    }

    /**
     * Collects snapshots of all waiting rooms that have not yet transitioned into active matches.
     *
     * @return A list of {@link MatchLobbyResponse} objects for open, joinable lobbies.
     */
    public List<MatchLobbyResponse> getOpenLobbies() {
        List<MatchLobbyResponse> openLobbies = new ArrayList<>();
        for(LobbyRoom room : waitingRooms.values()) {
            if(!room.isStarted()) {
                openLobbies.add(room.toResponse());
            }
        }
        return openLobbies;
    }

    /**
     * Seats a player into an existing waiting room and broadcasts the updated lobby state to all subscribers.
     * Triggers the 10-second auto-start countdown if this join fills the room to capacity.
     *
     * @param matchId  Unique identifier of the target waiting room.
     * @param playerId Identifier of the player joining the lobby.
     * @return The updated {@link MatchLobbyResponse} snapshot.
     * @throws IllegalArgumentException if no waiting room exists with {@code matchId}.
     * @throws IllegalStateException    if the lobby is full, already started, or the player ID is invalid/duplicate.
     */
    public MatchLobbyResponse joinLobby(String matchId, String playerId) {

        LobbyRoom room = waitingRooms.get(matchId);
        if(room == null) {throw new IllegalArgumentException("Lobby not found.");}

        boolean joined = room.addPlayer(playerId != null ? playerId.trim() : null);
        if(!joined) {throw new IllegalStateException("Unable to join lobby.");}

        MatchLobbyResponse response = room.toResponse();

        broadcastLobbyUpdate(matchId, response);

        return response;
    }

    /**
     * Removes a player from a waiting room before the match starts.
     * Deletes the room from memory if it becomes empty; otherwise broadcasts the updated state
     * (including any cancelled countdown) to remaining players.
     *
     * @param matchId  Unique identifier of the target waiting room.
     * @param playerId Identifier of the player leaving the lobby.
     * @return The updated {@link MatchLobbyResponse} snapshot prior to room cleanup.
     * @throws IllegalArgumentException if no waiting room exists with {@code matchId}.
     * @throws IllegalStateException    if the match has already started or the player is not in the room.
     */
    public MatchLobbyResponse leaveLobby(String matchId, String playerId) {

        LobbyRoom room = waitingRooms.get(matchId);
        if(room == null) {throw new IllegalArgumentException("Lobby not found.");}

        boolean removed = room.removePlayer(playerId != null ? playerId.trim() : null);
        if(!removed) {throw new IllegalStateException("Unable to leave lobby.");}

        MatchLobbyResponse response = room.toResponse();

        if(room.isEmpty()) {
            waitingRooms.remove(matchId);
        } else {
            broadcastLobbyUpdate(matchId, response);
        }
        return response;
    }

    /**
     * Allows the lobby creator to manually start the match before the 10-second countdown finishes,
     * provided at least 2 players are seated.
     *
     * @param matchId     Unique identifier of the target waiting room.
     * @param requesterId Identifier of the player requesting the early start (must match {@code hostPlayerId}).
     * @return The started {@link MatchLobbyResponse} snapshot, or {@code null} if already started.
     * @throws IllegalArgumentException if no waiting room exists with {@code matchId}.
     * @throws IllegalStateException    if the requester is not the host or fewer than 2 players are seated.
     */
    public MatchLobbyResponse startMatchByHost(String matchId, String requesterId) {

        LobbyRoom room = waitingRooms.get(matchId);
        if(room == null) {throw new IllegalArgumentException("Lobby not found.");}

        if(requesterId == null || !requesterId.equals(room.getHostPlayerId())) {throw new IllegalStateException("Only the host can start the match before countdown ends.");}

        if(room.getPlayerIds().size() < 2) {throw new IllegalStateException("At least 2 players are required to start.");}

        return startMatch(matchId);
    }


    /**
     * Atomically transitions a waiting room into a live {@link GameMatch}.
     * Invoked either manually by {@link #startMatchByHost(String, String)} or automatically
     * by the scheduler when the 10-second full-room countdown expires.
     *
     * @param matchId Unique identifier of the lobby transitioning to active gameplay.
     * @return The final {@link MatchLobbyResponse} with {@code isStarted = true}, or {@code null} if already started.
     */
    public MatchLobbyResponse startMatch(String matchId) {

        LobbyRoom room = waitingRooms.get(matchId);

        if(room == null || !room.markStarted()) {return null;}

        List<String> shuffledDeck = generateShuffledDeck(room.getPairCount());
        createMatch(matchId, shuffledDeck, room.getPlayerIds());
        waitingRooms.remove(matchId);
        MatchLobbyResponse response = room.toResponse();
        broadcastLobbyUpdate(matchId, response);

        return response;
    }

    /**
     * Builds and randomly shuffles a deck containing two matching cards for each pair.
     *
     * @param pairCount Number of distinct pairs (16 to 24), producing {@code pairCount * 2} total cards.
     * @return A shuffled list of card identifier strings (e.g., {@code "CARD_1"} through {@code "CARD_16"}).
     */
    private List<String> generateShuffledDeck(int pairCount) {
        List<String> deck = new ArrayList<>(pairCount * 2);
        for (int i = 1; i <= pairCount; i++) {
            deck.add("CARD_" + i);
            deck.add("CARD_" + i);
        }

        Collections.shuffle(deck);

        return deck;
    }

    /**
     * Pushes the latest lobby state snapshot to all WebSocket clients subscribed to {@code /topic/lobby/{matchId}}.
     *
     * @param matchId  Unique identifier of the lobby channel.
     * @param response The lobby state DTO to serialize and broadcast.
     */
    private void broadcastLobbyUpdate(String matchId, MatchLobbyResponse response) {

        messagingTemplate.convertAndSend("/topic/lobby/" + matchId, response);
    }

    /**
     * 1. Creates a new GameMatch object using the passed parameters.
     * 2. Inserts newly created GameMatch object into activeMatches map using the matchId as the key.
     * 3. Returns the newly created GameMatch object to the caller.
     */
    public GameMatch createMatch(String matchId, List<String> shuffledDeck, List<String> playerIds) {
        GameMatch gameMatch = new GameMatch(matchId, shuffledDeck, playerIds);
        activeMatches.put(matchId, gameMatch);
        return gameMatch;
    }

    /**
     * 1. Queries the activeMatches collection using the matchId key.
     * 2. Returns the associated GameMatch instance found, or returns null if no match with that ID is found among activeMatches.
     */
    public GameMatch getMatch(String matchId) {
        return activeMatches.get(matchId);
    }

    /**
     * Removes the entry corresponding to the matchId key from activeMatches map entirely.
     * This frees up the server's RAM once a game ends or is abandoned.
     */
    public void removeMatch(String matchId) {
        activeMatches.remove(matchId);
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduler.shutdownNow();
    }

}
