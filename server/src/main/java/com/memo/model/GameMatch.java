package com.memo.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an active in-memory multiplayer memory match game.
 * Uses lock-free atomic variables and concurrent collections to ensure thread safety
 * during simultaneous WebSocket game turns.
 */
public class GameMatch {
    private final String matchId;

    // Server-side source of truth for card values. Array index matches grid position.
    private final List<String> secretDeck;

    // Tracks active card states: true = card is still in play; false = permanently REMOVED
    private final Map<Integer, Boolean> cardVisibilityMap;

    // Thread-safe map tracking Player ID -> Current Pair Score
    private final Map<String, AtomicInteger> playerScores;

    private final List<String> turnOrder;
    private final AtomicInteger currentTurnIndex;

    public GameMatch(String matchId, List<String> shuffledDeck, List<String> playerIds) {
        this.matchId = matchId;
        this.secretDeck = shuffledDeck;
        this.currentTurnIndex = new AtomicInteger(0);
        this.turnOrder = playerIds;

        // Use ConcurrentHashMap to allow multiple threads to safely query card presence
        this.cardVisibilityMap = new ConcurrentHashMap<>();
        for (int i = 0; i < shuffledDeck.size(); i++) {
            this.cardVisibilityMap.put(i, true);
        }

        this.playerScores = new ConcurrentHashMap<>();
        for (String playerId : playerIds) {
            this.playerScores.put(playerId, new AtomicInteger(0));
        }
    }

    // --- Core Architecture Pillars & Getters ---

    public String getMatchId() {
        return matchId;
    }

    public List<String> getTurnOrder() {
        return turnOrder;
    }

    /**
     * Determines which player's turn it is by applying a modulo wheel operation.
     */
    public String getActivePlayerId() {
        return turnOrder.get(currentTurnIndex.get() % turnOrder.size());
    }

    public int getPlayerScore(String playerId) {
        return playerScores.getOrDefault(playerId, new AtomicInteger(0)).get();
    }

    /**
     * Safely increments the player's score utilizing CPU-level CAS instructions.
     */
    public void incrementPlayerScore(String playerId) {
        if (playerScores.containsKey(playerId)) {
            playerScores.get(playerId).incrementAndGet();
        }
    }

    /**
     * Advances the turn indicator lock-free.
     */
    public void advanceTurn() {
        currentTurnIndex.incrementAndGet();
    }

    /**
     * Evaluates a turn request. Security check: Only returns the card values
     * if the requested cards are actually still validly on the board.
     */
    public boolean isCardInPlay(int cardIndex) {
        return cardVisibilityMap.getOrDefault(cardIndex, false);
    }

    public void removeCardFromPlay(int cardIndex) {
        cardVisibilityMap.put(cardIndex, false);
    }

    public String getCardValue(int cardIndex) {
        return secretDeck.get(cardIndex);
    }
}

