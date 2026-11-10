package com.memo.controller;

import com.memo.dto.CardFlipRequest;
import com.memo.dto.GameRoundUpdate;
import com.memo.service.LobbyManager;
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

        // TODO: Extract and Validate the Player Identity
        // TODO: Interact with the Gatekeeper Transaction Layer
        // TODO: Game State Processing (The Complex Layer)
        // TODO: Construct and Return the Update Payload

        return null;
    }

}
