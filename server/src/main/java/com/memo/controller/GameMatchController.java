package com.memo.controller;

import com.memo.dto.CardFlipRequest;
import com.memo.dto.GameRoundUpdate;
import com.memo.service.LobbyManager;
import com.memo.model.GameMatch;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Network gateway handling real-time WebSocket traffic for active game turns.
 * Intercepts inbound payload data packets, extracts secure session identities,
 * and handles state processing routing through the in-memory LobbyManager.
 */
@Controller
public class GameMatchController {

    private final LobbyManager lobbyManager;

    /**
     * Constructor Injection ensures clean dependency initialization
     * and framework decoupling. Spring Boot automatically wires up the running Bean.
     */
    public GameMatchController(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    /**
     * Handles card flip request payloads incoming from the network connection.
     * Extracts connection identity from the Principal object to enforce secure validation.
     *
     * @param matchId   The unique room ID parsed directly out of the URI routing path.
     * @param principal The server-validated network session authentication context.
     * @param request   The secure parameter-tampering protected client data packet.
     * @return The compiled broadcast update payload sent directly back to the message broker channel.
     */
    @MessageMapping("/match/{matchId}/flip")
    @SendTo("/topic/match/{matchId}")
    public GameRoundUpdate handleCardFlip(
            @DestinationVariable String matchId,
            Principal principal,
            CardFlipRequest request)  {

        // 0. Check if request JSON payload arrived empty. If yes - silently drop the request.
        if(request == null) {
            return  null;
        }

        // 1. Extracting and validating the Player Identity. Realization below:
        // 1.1 Check if user with requested name exists and authorized. If not - silently drop the request.
        if(principal == null || principal.getName() == null) {
            return null;
        }
        // 1.2 Storing the player ID locally without possibility to change it.
        final String playerId = principal.getName();
        // 1.3 Check if Match with requested Name exists. If not - silently drop the request.
        final GameMatch match = this.lobbyManager.getMatch(matchId);
        if(match == null) {
            return null;
        }
        // 1.4 Check if it actually this player's turn.
        if(!playerId.equals(match.getActivePlayerId())) {
            return null;
        }


        // 2. Interact with the Gatekeeper Transaction Layer.
        // 2.1 Extract the requested card index.
        final int cardIndex = request.getCardIndex();

        // 2.2 Guard against self-matching exploits (clicking the same card twice)
        if(match.isSameCardClicked(cardIndex)) {
            return null;
        }

        // 2.3 Guard against exploits of already flipped and removed cards
        final String cardValue = match.validateAndFlipCard(cardIndex);
        if(cardValue == null) {
            return null;
        }

        // 3. Game State Processing (The Complex Layer)

        // 3.1 Prepare tracking variables.
        final boolean isSecondCard = !match.isFirstCardFlip();

        boolean isMatch = false;
        String nextPlayerId = playerId;
        boolean isGameOver = false;


        if(!isSecondCard) {

            // 3.2 Processing first card flip.
            match.updateTurnState(cardIndex, cardValue);
        }
        else {

            // 3.3 Processing second card flip.
            final int firstCardIndex = match.getFirstCardIndex();
            isMatch = match.checkMatch(cardValue);
            match.updateTurnState(firstCardIndex, cardValue);

            // 3.3.1 If cards match to each other.
            if(isMatch) {
                match.removeCardFromPlay(firstCardIndex);
                match.removeCardFromPlay(cardIndex);
                match.incrementPlayerScore(playerId);
                isGameOver = match.isBoardCleared();
            }

            // 3.3.2 If cards don't match to each other.
            else {
                match.advanceTurn(); //Moves to the next turn and thus automatically changes active player (see method realization).
                nextPlayerId = match.getActivePlayerId();
            }

        }

        // 4. Construct and Return the Update Payload
        return new GameRoundUpdate(playerId, cardIndex, cardValue, isSecondCard, isMatch, nextPlayerId, match.getAllScores(), isGameOver);
    }

}
