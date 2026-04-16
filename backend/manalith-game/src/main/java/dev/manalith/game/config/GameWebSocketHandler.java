package dev.manalith.game.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * GameWebSocketHandler
 *
 * Handles the /ws/game endpoint for real-time game play.
 *
 * Message flow:
 *   Client → TextMessage (JSON GameActionEnvelope)
 *     → validate JWT / turn ownership / requestId idempotency
 *     → route to GameSessionService.handleAction()
 *
 *   Server → TextMessage (JSON GameEventEnvelope)
 *     via GameEventPublisher → registered WebSocketSessions per room
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    // TODO: inject GameSessionService, SessionRegistry, ObjectMapper

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // TODO: register session, send current GameStateDTO for the room
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO: deserialize → GameActionEnvelope → GameSessionService.handleAction()
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // TODO: mark player as disconnected, start reconnect grace timer
    }
}
