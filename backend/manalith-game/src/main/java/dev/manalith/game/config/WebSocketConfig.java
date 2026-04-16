package dev.manalith.game.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration.
 *
 * Registers the game action handler at /ws/game and the lobby handler
 * at /ws/lobby. Authentication is enforced via JwtHandshakeInterceptor.
 *
 * STOMP is intentionally not used for game traffic; game events use
 * a thin custom JSON envelope (see docs/PROTOCOL.md).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameHandler;
    private final LobbyWebSocketHandler lobbyHandler;

    public WebSocketConfig(GameWebSocketHandler gameHandler,
                           LobbyWebSocketHandler lobbyHandler) {
        this.gameHandler  = gameHandler;
        this.lobbyHandler = lobbyHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameHandler, "/ws/game")
                .addInterceptors(new JwtHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(lobbyHandler, "/ws/lobby")
                .addInterceptors(new JwtHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}
