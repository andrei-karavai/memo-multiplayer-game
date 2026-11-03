package com.memo.dto;

import java.util.Map;

public class GameRoundUpdate {

    //Keeps the ID of the player, who is making his turn right now.
    private String playerID;

    //Keeps the ID of the card, that should be flipped during the actual part of the turn.
    private int cardIndex;

    //Keeps the value of the card, that should be flipped during the actual part of the turn.
    private String cardValue;

    //Keeps the check, whether currently flipped card is the first or the second card for current turn.
    private boolean isSecondCard;

    //Keeps the check, whether the first and the second cards flipped in current turn match to one another.
    private boolean isMatch;

    //Keeps the ID of the player, who gets the next turn, if current turn doesn't reveal cards that match one another.
    private String nextPlayerID;

    //Keeps the actual scores for each player.
    private Map<String, Integer> scores;

    //Keeps the check, if game is over as a result of current turn (All the cards are revealed and assigned to players).
    private boolean isGameOver;

    /**
     * Initializes a new game round update without defining any parameters.
     */
    public GameRoundUpdate() {}

    /**
     * Initializes a new game round update with parameters
     *
     * @param playerID      ID of the player, who is making the turn now.
     * @param cardIndex     ID of the card, that should be flipped during the actual part of the turn.
     * @param cardValue     Value of the card, that should be flipped during the actual part of the turn.
     * @param isSecondCard  Flag, whether currently flipped card is the first or the second card for current turn.
     * @param isMatch       Flag, whether the first and the second cards flipped in current turn match to one another.
     * @param nextPlayerID  ID of the player, who gets the next turn, if current turn doesn't reveal cards that match one another.
     * @param scores        Map with actual scores for each participating player.
     * @param isGameOver    Flag, whether the game is over as a result of current turn.
     */
    public GameRoundUpdate(String playerID,
                           int cardIndex,
                           String cardValue,
                           boolean isSecondCard,
                           boolean isMatch,
                           String nextPlayerID,
                           Map<String, Integer> scores,
                           boolean isGameOver) {
        this.playerID = playerID;
        this.cardIndex = cardIndex;
        this.cardValue = cardValue;
        this.isSecondCard = isSecondCard;
        this.isMatch = isMatch;
        this.nextPlayerID = nextPlayerID;
        this.scores = scores;
        this.isGameOver = isGameOver;
    }

    /**
     * Fetches the ID of the ID of the player, who is making the turn now.
     */
    public String getPlayerID() {
        return this.playerID;
    }

    /**
     * Fetches the ID of the card, that should be flipped during the actual part of the turn.
     */
    public int getCardIndex() {
        return this.cardIndex;
    }

    /**
     * Fetches the value of the card, that should be flipped during the actual part of the turn.
     */
    public String getCardValue() {
        return this.cardValue;
    }

    /**
     * Fetches the status, whether it is first or the second card to be flipped in current turn.
     */
    public boolean isSecondCard() {
        return this.isSecondCard;
    }

    /**
     * Fetches the status, whether two cards flipped in current turn match to one another.
     */
    public boolean isMatch() {
        return this.isMatch;
    }

    /**
     * Fetches the ID of the player, who gets the next turn, if current turn doesn't reveal cards that match one another.
     */
    public String getNextPlayerID() {
        return this.nextPlayerID;
    }

    /**
     * Fetches the map with actual scores for each participating player.
     */
    public Map<String, Integer> getScores() {
        return this.scores;
    }

    /**
     * Fetches the status, whether the game is over as a result of current turn.
     */
    public boolean isGameOver() {
        return isGameOver;
    }


    //Standard setters for the case of object creation using default constructor.
    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public void setCardIndex(int cardIndex) {
        this.cardIndex = cardIndex;
    }

    public void setCardValue(String cardValue) {
        this.cardValue = cardValue;
    }

    public void setSecondCard(boolean secondCard) {
        this.isSecondCard = secondCard;
    }

    public void setMatch(boolean match) {
        this.isMatch = match;
    }

    public void setNextPlayerID(String nextPlayerID) {
        this.nextPlayerID = nextPlayerID;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }
}
