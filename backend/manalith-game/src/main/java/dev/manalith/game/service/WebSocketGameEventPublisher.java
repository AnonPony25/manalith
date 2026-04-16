package dev.manalith.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.forge_adapter.DecisionRequestDTO;
import dev.manalith.forge_adapter.GameDiffDTO;
import dev.manalith.forge_adapter.GameEventPublisher;
import dev.manalith.forge_adapter.GameOutcomeDTO;
import dev.manalith.forge_adapter.GameStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketGameEventPublisher
 *
 * Spring {@code @Component} that implements {@link GameEventPublisher} by
 * broadcasting serialized JSON messages to connected {@link WebSocketSession}s.
 *
 * <p>Sessions are tracked per room in {@link #roomSessions}. Each room maps to a
 * set of live WebSocket sessions (one per connected player or spectator).
 *
 * <p>All outbound messages are serialized via Jackson's {@link ObjectMapper} and
 * wrapped in a typed envelope:
 * <pre>
 * {
 *   "type": "GAME_STATE" | "GAME_DIFF" | "DECISION_REQUEST" | ...,
 *   "payload": { ... }
 * }
 * </pre>
 *
 * <p>Sessions that have been closed are silently pruned during any send attempt.
 *
 * @see GameEventPublisher
 */
@Component
public class WebSocketGameEventPublisher implements GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketGameEventPublisher.class);

    /**
     * Registry: roomId → set of active WebSocket sessions for that room.
     * Concurrent because sessions are registered/unregistered from the WebSocket
     * I/O thread while messages are sent from the Forge game thread.
     */
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> roomSessions =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public WebSocketGameEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ─── Session registry ─────────────────────────────────────────────────────

    /**
     * Registers a WebSocket session for a room/seat combination.
     * Called by {@link dev.manalith.game.config.GameWebSocketHandler#afterConnectionEstablished}.
     *
     * @param roomId  The room UUID this session is joining.
     * @param seatId  The seat UUID identifying the player (stored in session attributes).
     * @param session The newly established WebSocket session.
     */
    public void registerSession(UUID roomId, UUID seatId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId,
                k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
        log.info("Registered WebSocket session {} for room={} seat={}", session.getId(), roomId, seatId);
    }

    /**
     * Removes a WebSocket session from the room registry.
     * Called by {@link dev.manalith.game.config.GameWebSocketHandler#afterConnectionClosed}.
     *
     * @param roomId  The room UUID.
     * @param session The session that closed.
     */
    public void unregisterSession(UUID roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        log.info("Unregistered WebSocket session {} from room={}", session.getId(), roomId);
    }

    // ─── GameEventPublisher implementation ───────────────────────────────────

    /**
     * Broadcasts a full game-state snapshot to all sessions in the room.
     * Emits event type {@code GAME_STATE}.
     *
     * @param roomId The room to broadcast to.
     * @param state  The full game-state DTO.
     */
    @Override
    public void emitGameState(UUID roomId, GameStateDTO state) {
        broadcast(roomId, buildEnvelope("GAME_STATE", state));
    }

    /**
     * Broadcasts an incremental game-state diff to all sessions in the room.
     * Emits event type {@code GAME_DIFF}.
     *
     * @param roomId The room to broadcast to.
     * @param diff   The diff DTO.
     */
    @Override
    public void emitGameDiff(UUID roomId, GameDiffDTO diff) {
        broadcast(roomId, buildEnvelope("GAME_DIFF", diff));
    }

    /**
     * Sends a decision request to a specific seat/player.
     * Emits event type {@code DECISION_REQUEST}.
     *
     * <p>Only the session whose attributes contain {@code seatId} will receive
     * the targeted message; all others in the room are skipped.
     *
     * @param roomId  The room UUID.
     * @param seatId  The seat that must respond.
     * @param request The decision request payload.
     */
    @Override
    public void emitDecisionRequest(UUID roomId, UUID seatId, DecisionRequestDTO request) {
        sendToSeat(roomId, seatId, buildEnvelope("DECISION_REQUEST", request));
    }

    /**
     * Sends an action-rejected notice to the specified seat.
     * Emits event type {@code ACTION_REJECTED}.
     *
     * @param roomId The room UUID.
     * @param seatId The seat that sent the invalid action.
     * @param reason Human-readable explanation.
     */
    @Override
    public void emitActionRejected(UUID roomId, UUID seatId, String reason) {
        sendToSeat(roomId, seatId,
                buildEnvelope("ACTION_REJECTED", Map.of("reason", reason)));
    }

    /**
     * Broadcasts a game-over notification to all sessions in the room.
     * Emits event type {@code GAME_OVER}.
     *
     * @param roomId  The room UUID.
     * @param outcome The final outcome DTO.
     */
    @Override
    public void emitGameOver(UUID roomId, GameOutcomeDTO outcome) {
        broadcast(roomId, buildEnvelope("GAME_OVER", outcome));
    }

    /**
     * Sends a priority prompt to the specified seat.
     * Emits event type {@code PRIORITY_CHANGED}.
     *
     * @param roomId The room UUID.
     * @param seatId The seat that now holds priority.
     */
    @Override
    public void emitPriorityPrompt(UUID roomId, UUID seatId) {
        sendToSeat(roomId, seatId,
                buildEnvelope("PRIORITY_CHANGED", Map.of("seatId", seatId)));
    }

    /**
     * Sends a timeout warning to the specified seat (15-second auto-pass warning).
     * Emits event type {@code DECISION_TIMEOUT_WARNING}.
     *
     * @param roomId  The room UUID.
     * @param seatId  The seat about to be auto-passed.
     * @param context Human-readable label for the decision context.
     */
    @Override
    public void emitTimeoutWarning(UUID roomId, UUID seatId, String context) {
        sendToSeat(roomId, seatId,
                buildEnvelope("DECISION_TIMEOUT_WARNING",
                        Map.of("seatId", seatId, "context", context)));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Sends a JSON message to all active sessions in a room.
     * Closed sessions are pruned silently.
     */
    private void broadcast(UUID roomId, String json) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            sendSafe(session, msg);
        }
    }

    /**
     * Sends a JSON message only to the session whose {@code "seatId"} attribute
     * matches the given seatId.
     */
    private void sendToSeat(UUID roomId, UUID seatId, String json) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            Object attr = session.getAttributes().get("seatId");
            if (seatId.toString().equals(attr != null ? attr.toString() : null)) {
                sendSafe(session, msg);
                return;
            }
        }
        log.warn("No session found for seatId={} in room={}", seatId, roomId);
    }

    /**
     * Sends a {@link TextMessage} to a session, logging and pruning on error.
     */
    private void sendSafe(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message to session {}: {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Serializes a typed payload into a JSON event envelope.
     *
     * <p>Envelope structure:
     * <pre>
     * { "type": "&lt;eventType&gt;", "payload": &lt;payload&gt; }
     * </pre>
     *
     * @param eventType The {@code type} discriminator string.
     * @param payload   The payload object (will be serialized via Jackson).
     * @return Serialized JSON string.
     */
    private String buildEnvelope(String eventType, Object payload) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", eventType,
                    "payload", payload
            ));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event type={}: {}", eventType, e.getMessage());
            return "{\"type\":\"" + eventType + "\",\"payload\":null}";
        }
    }
}
