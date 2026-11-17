package com.memo.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Lazy;

import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import org.springframework.http.server.ServerHttpRequest;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.memo.service.LobbyManager;


/**
 * Configures STOMP over WebSocket messaging for real-time multiplayer gameplay.
 *
 * <ul>
 *   <li><b>Handshake Endpoint ({@code /ws}):</b> Initial HTTP upgrade URL for client connections.</li>
 *   <li><b>Application Prefix ({@code /app}):</b> Routes inbound player actions to {@code @MessageMapping} controllers.</li>
 *   <li><b>Broker Prefix ({@code /topic}):</b> Broadcasts match state updates to all subscribed players.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final LobbyManager lobbyManager;

    /**
     * Injects {@link LobbyManager} to notify lobby and match timers on player connect/disconnect events.
     *
     * @param lobbyManager Centralized lobby and active match service.
     */
    public WebSocketConfig(@Lazy LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }





    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO (Module 5): Restrict allowed origin patterns to the exact production frontend domain.
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(
                            ServerHttpRequest request,
                            WebSocketHandler wsHandler,
                            Map<String, Object> attributes) {
                        String playerId = UriComponentsBuilder.fromUri(request.getURI())
                                .build()
                                .getQueryParams()
                                .getFirst("playerId");
                        if (playerId == null || playerId.isBlank()) {
                            return null;
                        }
                        final String cleanId = playerId.trim();
                        return () -> cleanId;
                    }
                })
                .setAllowedOriginPatterns("*");
    }

    /**
     * Cancels any active 60-second disconnect grace timer when a player establishes a STOMP session.
     *
     * @param event Spring STOMP session connected event carrying the bound {@link Principal}.
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null && user.getName() != null) {
            lobbyManager.handlePlayerReconnect(user.getName());
        }
    }

    /**
     * Triggers immediate lobby removal or a 60-second match disconnect grace timer when a STOMP session closes.
     *
     * @param event Spring STOMP session disconnect event carrying the bound {@link Principal}.
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null && user.getName() != null) {
            lobbyManager.handlePlayerDisconnect(user.getName());
        }
    }


}
