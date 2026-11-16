package com.memo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.memo.dto.CreateMatchRequest;
import com.memo.dto.MatchLobbyResponse;
import com.memo.service.LobbyManager;



/**
 * REST API gateway handling pre-game lobby lifecycle operations over HTTP.
 * Exposes endpoints for creating lobbies, listing open waiting rooms, joining/leaving,
 * and host-initiated early match starts, delegating state transitions to {@link LobbyManager}.
 */
@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    // Centralized service managing waiting rooms, countdown timers, and active match transitions.
    private final LobbyManager lobbyManager;

    /**
     * Initializes the REST controller with the singleton {@link LobbyManager} service via constructor injection.
     *
     * @param lobbyManager The thread-safe lobby and match lifecycle manager.
     */
    public LobbyController(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    /**
     * Creates a new waiting room lobby and seats the requesting player as host.
     *
     * @param createMatchRequest JSON payload specifying host player ID, card pair count (16–24), and max players (2–4).
     * @return A {@link MatchLobbyResponse} snapshot of the newly created lobby.
     */
    @PostMapping("/create")
    public MatchLobbyResponse createLobby(@RequestBody CreateMatchRequest createMatchRequest) {
        return lobbyManager.createLobby(createMatchRequest);
    }

    /**
     * Retrieves all open waiting rooms that have not yet started.
     *
     * @return A list of {@link MatchLobbyResponse} snapshots for joinable lobbies.
     */
    @GetMapping
    public List<MatchLobbyResponse> getOpenLobbies() {
        return lobbyManager.getOpenLobbies();
    }


    /**
     * Seats a player into an existing waiting room and triggers the 10-second auto-start countdown if full.
     *
     * @param matchId  Unique 6-character room code parsed from the URL path.
     * @param playerId Identifier of the player joining the lobby.
     * @return The updated {@link MatchLobbyResponse} snapshot.
     */
    @PostMapping("/{matchId}/join")
    public MatchLobbyResponse joinLobby(@PathVariable String matchId, @RequestParam String playerId) {
        return lobbyManager.joinLobby(matchId, playerId);
    }

    /**
     * Removes a seated player from a waiting room before the match starts and cancels any active countdown.
     *
     * @param matchId  Unique 6-character room code parsed from the URL path.
     * @param playerId Identifier of the player leaving the lobby.
     * @return The updated {@link MatchLobbyResponse} snapshot prior to room removal if empty.
     */
    @PostMapping("/{matchId}/leave")
    public MatchLobbyResponse leaveLobby(@PathVariable String matchId, @RequestParam String playerId) {
        return lobbyManager.leaveLobby(matchId, playerId);
    }

    /**
     * Allows the lobby host to manually start the match early when at least 2 players are seated.
     *
     * @param matchId     Unique 6-character room code parsed from the URL path.
     * @param requesterId Identifier of the player requesting the start (must match {@code hostPlayerId}).
     * @return The started {@link MatchLobbyResponse} snapshot with {@code isStarted = true}.
     */
    @PostMapping("/{matchId}/start")
    public MatchLobbyResponse startMatch(@PathVariable String matchId, @RequestParam String requesterId) {
        return lobbyManager.startMatchByHost(matchId, requesterId);
    }

    /**
     * Translates invalid input or missing-lobby exceptions into HTTP 400 Bad Request JSON error responses.
     *
     * @param ex The caught {@link IllegalArgumentException} thrown by the service layer.
     * @return A {@link ResponseEntity} containing HTTP 400 and an {@code {"error": "..."}} JSON payload.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.
               status(HttpStatus.BAD_REQUEST).
               body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Invalid request."));
    }

    /**
     * Translates invalid lobby-state or unauthorized-action exceptions into HTTP 409 Conflict JSON error responses.
     *
     * @param ex The caught {@link IllegalStateException} thrown by the service layer.
     * @return A {@link ResponseEntity} containing HTTP 409 and an {@code {"error": "..."}} JSON payload.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.
               status(HttpStatus.CONFLICT).
               body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Action not allowed in current lobby state."));
    }

}
