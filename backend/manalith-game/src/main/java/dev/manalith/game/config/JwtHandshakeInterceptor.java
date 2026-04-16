package dev.manalith.game.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * JwtHandshakeInterceptor
 *
 * Validates the JWT bearer token on WebSocket upgrade requests.
 * Token is expected in query param ?token=<jwt> or Authorization header.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // TODO: extract and validate JWT; populate attributes with userId + roomId
        return true; // allow all in dev; tighten before Phase 1
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {}
}
