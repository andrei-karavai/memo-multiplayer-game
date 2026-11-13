package com.memo.dto;


public class CreateMatchRequest {

    // ID (name) of the player (user) creating the match.
    private String hostPlayerId;

    // Number of card pairs for this match, minimum 16 to maximum 24 pairs.
    private int pairCount;

    // Number of maximum allowed participating players in this match. Minimum 2 to maximum 4.
    private int maxPlayers;

    /**
     * Initializes a new match request without defining parameters of the match.
     */
    public CreateMatchRequest() {}

    /**
     * Initializes a new match request with defined parameters of the match.
     *
     * @param hostPlayerId  ID (name) of the player (user) creating the current match.
     * @param pairCount     Number of card pairs for current match, minimum 16 to maximum 24 pairs.
     * @param maxPlayers    Number of maximum participating players in current match. Minimum 2 to maximum 4.
     */
    public CreateMatchRequest(String hostPlayerId, int pairCount, int maxPlayers) {
        this.hostPlayerId = hostPlayerId;
        this.maxPlayers = maxPlayers;
        this.pairCount = pairCount;
    }

    /**
     * Fetches ID (name) of the player, which created current match.
     */
    public String getHostPlayerId() { return this.hostPlayerId;}

    /**
     * Fetches the number of card pairs for current match.
     */
    public int getPairCount() {return this.pairCount;}

    /**
     * Fetches the number maximum allowed participation players for current match.
     */
    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    /**
     * Sets ID (name) of the player, who creates (and owns) the current match.
     */
    public void setHostPlayerId(String hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    /**
     * Sets the number of card pairs for current match.
     */
    public void setPairCount(int pairCount) {
        this.pairCount = pairCount;
    }

    /**
     * Sets he number maximum allowed participation players for current match.
     */
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

}
