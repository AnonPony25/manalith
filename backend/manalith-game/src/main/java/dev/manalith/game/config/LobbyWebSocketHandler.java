package dev.manalith.game.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * LobbyWebSocketHandler
 *
 * Handles /ws/lobby for room discovery, ready-checks, chat, and
 * game-start coordination before switching to /ws/game.
 */
@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // TODO: route lobby events (JOIN_ROOM, READY, INVITE, CHAT, etc.)
    }
}
