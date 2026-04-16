package dev.manalith.forge_adapter;

import java.util.UUID;

/**
 * GameEventPublisher
 *
 * Abstraction for broadcasting game events to connected WebSocket clients.
 * Implemented in manalith-game by a Spring-managed bean that holds
 * the SimpMessagingTemplate / WebSocket session registry.
 */
public interface GameEventPublisher {

    void emitPriorityPrompt(UUID roomId, UUID seatId);

    void emitTimeoutWarning(UUID roomId, UUID seatId, String context);

    void emitGameState(UUID roomId, GameStateDTO state);

    void emitGameDiff(UUID roomId, GameDiffDTO diff);

    void emitDecisionRequest(UUID roomId, UUID seatId, DecisionRequestDTO request);

    void emitActionRejected(UUID roomId, UUID seatId, String reason);

    void emitGameOver(UUID roomId, GameOutcomeDTO outcome);
}
