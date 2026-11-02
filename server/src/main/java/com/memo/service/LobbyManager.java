package com.memo.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.memo.model.GameMatch;
import org.springframework.stereotype.Service;

@Service
public class LobbyManager {

    //Tracks active matches available on server.
    private final Map<String, GameMatch> activeMatches;

    /**
     * Initializes a new LobbyManager.
     */

    public LobbyManager() {
        this.activeMatches = new ConcurrentHashMap<>();
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

}
