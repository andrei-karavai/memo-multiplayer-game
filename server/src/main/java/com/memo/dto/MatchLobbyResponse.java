package com.memo.dto;

import java.util.List;

public class MatchLobbyResponse {

    // Unique room (match) code/ID.
    private String matchId;

    // ID (name) of the player (user) creating the match.
    private String hostPlayerId;

    // IDs (names) of all the players (users) currently seated in the waiting-room.
    private List<String> playerIds;

    // Number of maximum allowed participating players in this match. Minimum 2 to maximum 4.
    private int maxPlayers;

    // Number of cards for this match, even, minimum 32 to maximum 48 cards.
    private int totalCards;

    // True, once the match begins (either automatically when full and start-countdown is out or when the host triggers early start).
    private boolean isStarted;

    // Time for automatic game start, after the players' number reach maximum in current match.
    private Long countdownEndEpochMs;

    /**
     * Initializes a new response for match lobby without defining parameters of the match.
     */
    public MatchLobbyResponse() {}

    /**
     * Initializes a new response for match lobby with defined parameters of the match.
     *
     * @param matchId               Unique room (match) code/ID.
     * @param hostPlayerId          ID (name) of the player (user) creating the current match.
     * @param playerIds             List of all the players (users) currently seated in the waiting-room.
     * @param maxPlayers            Number of maximum allowed participating players in this match. Minimum 2 to maximum 4.
     * @param totalCards            Number of cards for current match, even, minimum 32 to maximum 48 cards.
     * @param isStarted             Flag, whether the match has begun (either automatically when full and start-countdown is out or when the host triggers early start).
     * @param countdownEndEpochMs   Time for automatic game start, after the players' number reach maximum in current match.
     */
    public MatchLobbyResponse(String matchId,
                              String hostPlayerId,
                              List<String> playerIds,
                              int maxPlayers,
                              int totalCards,
                              boolean isStarted,
                              Long countdownEndEpochMs) {
        this.matchId = matchId;
        this.hostPlayerId = hostPlayerId;
        this.playerIds = playerIds;
        this.maxPlayers = maxPlayers;
        this.totalCards = totalCards;
        this.isStarted = isStarted;
        this.countdownEndEpochMs = countdownEndEpochMs;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public void setHostPlayerId(String hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<String> playerIds) {
        this.playerIds = playerIds;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(int totalCards) {
        this.totalCards = totalCards;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public Long getCountdownEndEpochMs() {
        return countdownEndEpochMs;
    }

    public void setCountdownEndEpochMs(Long countdownEndEpochMs) {
        this.countdownEndEpochMs = countdownEndEpochMs;
    }
}
