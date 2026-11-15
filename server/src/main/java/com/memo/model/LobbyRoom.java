package com.memo.model;

import com.memo.dto.MatchLobbyResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe in-memory model representing a pre-game waiting room (lobby).
 * Manages player seating, capacity checks, and an automatic 10-second start
 * countdown triggered once the room reaches {@code maxPlayers}.
 */
public class LobbyRoom {

    // Countdown duration before match starts automatically, when maximum allowed number of players is already waiting in the lobby.
    public static final long AUTO_START_DELAY_MS = 10_000L;

    // Unique room (match) code/ID.
    private final String matchId;

    // ID (name) of the player (user) creating the match.
    private final String hostPlayerId;

    // Number of card pairs for this match, minimum 16 to maximum 24 pairs.
    private final int pairCount;

    // Number of maximum allowed participating players in this match. Minimum 2 to maximum 4.
    private final int maxPlayers;

    // IDs (names) of all the players (users) currently seated in the waiting-room. Host comes at the beginning of the list.
    private final List<String> playerIds;

    // Shared timer engine passed in from LobbyManager.
    private final ScheduledExecutorService scheduler;

    // Action executed when the 10 seconds countdown before match finishes.
    private final Runnable onAutoStartCallback;

    // True, once the match begins (either automatically when full and start-countdown is out or when the host triggers early start).
    private boolean isStarted;

    // Time for automatic game start, after the players' number reach maximum in current match.
    private Long countdownEndEpochMs;

    // Mechanism to execute the auto-start countdown.
    private ScheduledFuture<?> countdownTask;

    /**
     * Initializes a new waiting-room lobby and seats the creating host as the first player.
     *
     * @param matchId             Unique room identifier code.
     * @param hostPlayerId        Player ID (username) of the lobby creator.
     * @param pairCount           Number of card pairs configured for this match (16 to 24).
     * @param maxPlayers          Maximum allowed players in this lobby (2 to 4).
     * @param scheduler           Shared executor service used to schedule the auto-start countdown.
     * @param onAutoStartCallback Callback executed by the scheduler when the countdown expires.
     */
    public LobbyRoom(String matchId,
                     String hostPlayerId,
                     int pairCount,
                     int maxPlayers,
                     ScheduledExecutorService scheduler,
                     Runnable onAutoStartCallback) {

        this.matchId = matchId;
        this.hostPlayerId = hostPlayerId;
        this.pairCount = pairCount;
        this.maxPlayers = maxPlayers;
        this.scheduler = scheduler;
        this.onAutoStartCallback = onAutoStartCallback;

        this.playerIds = new ArrayList<>();
        this.playerIds.add(0,hostPlayerId);

        this.isStarted = false;
        this.countdownEndEpochMs = null;
        this.countdownTask = null;

    }

    /**
     * Attempts to seat a player in the waiting room.
     * Automatically starts the 10-second countdown if this join fills the room to {@code maxPlayers}.
     *
     * @param playerId ID of the player attempting to join.
     * @return {@code true} if the player was added; {@code false} if the match already started,
     *         the ID is invalid/duplicate, or the room is full.
     */

    public synchronized boolean addPlayer(String playerId) {
        if(isStarted ||
           playerId == null ||
           playerId.isBlank() ||
           playerIds.contains(playerId) ||
           playerIds.size() >= maxPlayers)
        {return false;}

        playerIds.add(playerId);
        if(playerIds.size() == maxPlayers)
        {startCountdown();}
        return true;

    }

    /**
     * Removes a seated player from the waiting room before the match starts.
     * Cancels any active auto-start countdown since the room is no longer at capacity.
     *
     * @param playerId ID of the player leaving the lobby.
     * @return {@code true} if the player was removed; {@code false} if the match already started
     *         or the player was not in the room.
     */

    public synchronized boolean removePlayer(String playerId) {
        if(isStarted ||
           playerId == null ||
           !playerIds.contains(playerId))
        {return false;}

        playerIds.remove(playerId);
        cancelCountdown();
        return true;
    }

    /**
     * Atomically transitions the lobby from waiting to started.
     * Prevents race conditions between a manual host start and the 10-second timer firing simultaneously.
     *
     * @return {@code true} if this call transitioned the room to started; {@code false} if already started.
     */
    public synchronized boolean markStarted() {
        if (isStarted) {return false;}
        isStarted = true;
        cancelCountdown();
        return true;
    }

    /**
     * Builds an immutable, broadcast-ready snapshot DTO of the current lobby state.
     * Copies {@code playerIds} defensively so JSON serialization stays thread-safe outside the monitor lock.
     *
     * @return A {@link MatchLobbyResponse} snapshot for REST responses or STOMP topic broadcasts.
     */
    public synchronized MatchLobbyResponse toResponse() {
        return new MatchLobbyResponse(matchId, hostPlayerId, new ArrayList<>(playerIds), maxPlayers, pairCount * 2, isStarted, countdownEndEpochMs);
    }

    public synchronized String getMatchId() {return matchId;}

    public synchronized String getHostPlayerId() {return hostPlayerId;}

    public synchronized int getPairCount() {return pairCount;}

    public synchronized int getMaxPlayers() {return maxPlayers;}

    public synchronized List<String> getPlayerIds() {return new ArrayList<>(playerIds);}

    public synchronized boolean isStarted() {return isStarted;}

    public synchronized boolean isEmpty() {return playerIds.isEmpty();}


    /**
     * Cancels any active auto-start timer task and clears {@code countdownEndEpochMs}.
     */
    private void cancelCountdown() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel(false);
            this.countdownTask = null;
        }
        this.countdownEndEpochMs = null;

    }

    /**
     * Records the target start epoch timestamp and schedules {@code onAutoStartCallback}
     * after {@link #AUTO_START_DELAY_MS} milliseconds.
     */
    private void startCountdown() {
        cancelCountdown();
        this.countdownEndEpochMs = System.currentTimeMillis() +  AUTO_START_DELAY_MS;
        this.countdownTask = scheduler.schedule(onAutoStartCallback, AUTO_START_DELAY_MS, TimeUnit.MILLISECONDS);
    }

}
