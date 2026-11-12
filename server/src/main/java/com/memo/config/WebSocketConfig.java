package com.memo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // TODO (Module 5): Restrict allowed origin patterns to the exact production frontend domain.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
