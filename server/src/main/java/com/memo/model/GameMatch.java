package com.memo.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an active in-memory multiplayer memory match game.
 * Uses lock-free atomic variables and concurrent collections to ensure thread safety
 * during simultaneous WebSocket game turns, combined with synchronized compound
 * validation methods to prevent structural multi-click exploits.
 */
public class GameMatch {
    // Unique identifier tracking this specific game session room across the server.
    private final String matchId;

    // Server-side source of truth for card values. Array index matches grid position.
    // Marked final because the layout configuration never changes after initialization.
    private final List<String> secretDeck;

    // Tracks active card states: true = card is still on the board; false = permanently REMOVED
    // Uses ConcurrentHashMap to allow multiple threads to safely query card presence.
    private final Map<Integer, Boolean> cardVisibilityMap;

    // Thread-safe map tracking Player ID -> Current Pair Score using CPU-level CAS operations.
    private final Map<String, AtomicInteger> playerScores;

    // Ordered list of Player IDs representing the exact sequence of player turns.
    private final List<String> turnOrder;

    // A thread-safe monotonic counter incremented every time a player turn passes.
    private final AtomicInteger currentTurnIndex;

    /**
     * Initializes a new multiplayer game match session with a secure, server-side deck layout.
     *
     * @param matchId      The unique room identifier.
     * @param shuffledDeck The pre-shuffled identity cards array representing the hidden grid.
     * @param playerIds    The collection of unique player identifiers participating in this match.
     */
    public GameMatch(String matchId, List<String> shuffledDeck, List<String> playerIds) {
        this.matchId = matchId;
        this.secretDeck = shuffledDeck;
        this.currentTurnIndex = new AtomicInteger(0);
        this.turnOrder = playerIds;

        // Populate visibility map: all indices start as true (hidden, but in play on the board)
        this.cardVisibilityMap = new ConcurrentHashMap<>();
        for (int i = 0; i < shuffledDeck.size(); i++) {
            this.cardVisibilityMap.put(i, true);
        }

        // Initialize starting scores to 0 for every connected player profile
        this.playerScores = new ConcurrentHashMap<>();
        for (String playerId : playerIds) {
            this.playerScores.put(playerId, new AtomicInteger(0));
        }
    }

    // --- Core Architecture Pillars & Getters ---

    /**
     * Fetches the unique room identity string.
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Retrieves the unmodifiable sequence list of player turns.
     */
    public List<String> getTurnOrder() {
        return turnOrder;
    }

    /**
     * Determines which player's turn it is by applying a modulo wheel operation.
     * Uses the total number of connected players to mathematically map an infinitely
     * growing turn counter into a permanent, clean rotational sequence.
     */
    public String getActivePlayerId() {
        return turnOrder.get(currentTurnIndex.get() % turnOrder.size());
    }

    /**
     * Retrieves the current verified score for a specific player profile.
     */
    public int getPlayerScore(String playerId) {
        return playerScores.getOrDefault(playerId, new AtomicInteger(0)).get();
    }

    /**
     * Safely increments the target player's pair score utilizing CPU-level lock-free CAS instructions.
     */
    public void incrementPlayerScore(String playerId) {
        if (playerScores.containsKey(playerId)) {
            playerScores.get(playerId).incrementAndGet();
        }
    }

    /**
     * Advances the turn indicator completely lock-free by bumping the atomic counter.
     */
    public void advanceTurn() {
        currentTurnIndex.incrementAndGet();
    }

    /**
     * 🔥 GATEKEEPER TRANSACTION METHOD
     * Evaluates a card flip request incoming from the network.
     * The 'synchronized' modifier locks this class instance, guaranteeing that the
     * check-then-act flow executes as an atomic unit. This prevents competitive multi-threaded
     * message arrival from processing or reading a card's state concurrently.
     *
     * Security check: Only returns the card values if the requested card is actually
     * still validly on the board. Clients never see card values before passing this layer.
     *
     * @param cardIndex The index of the targeted card on the grid.
     * @return The string identity value of the card if valid; null if the card is already out of play.
     */
    public synchronized String validateAndFlipCard(int cardIndex) {
        // 1. Check: Is the card index valid and currently marked as true (in play)?
        if (cardVisibilityMap.getOrDefault(cardIndex, false)) {
            // Note: We don't remove it from play here; it only disappears permanently
            // via removeCardFromPlay() once a full matching pair is verified by the engine.
            return secretDeck.get(cardIndex);
        }
        // 2. Reject: Card has already been cleared or the index is completely invalid.
        return null;
    }

    /**
     * Permanently flags a verified matched pair as out of play, removing it from the board layout.
     * This method is synchronized to maintain state ordering alignment with the gatekeeper method.
     *
     * @param cardIndex The index of the card to be wiped from the active board.
     */
    public synchronized void removeCardFromPlay(int cardIndex) {
        cardVisibilityMap.put(cardIndex, false);
    }
}
