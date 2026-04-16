package dev.manalith.forge_adapter;

import java.util.List;
import java.util.UUID;

/**
 * DecisionRequestDTO
 *
 * Wire payload sent to the client when Forge is awaiting a specific decision.
 * This corresponds to the {@code DECISION_REQUEST} event type defined in
 * {@code docs/PROTOCOL.md}.
 *
 * <p>The client must respond with a {@code GAME_ACTION} whose {@code requestId}
 * matches this DTO's {@code requestId} field, ensuring idempotent delivery and
 * replay protection.
 *
 * @param requestId      Unique ID for this decision prompt (used for correlation).
 * @param seatId         The seat/player this decision is addressed to.
 * @param kind           Decision kind matching a {@code payload.kind} from the protocol
 *                       (e.g. {@code CHOOSE_TARGETS}, {@code DECLARE_ATTACKERS},
 *                       {@code MULLIGAN_DECISION}).
 * @param prompt         Human-readable description of what is being decided.
 * @param options        List of valid option IDs or labels the client may choose from.
 *                       Empty for free-form numeric decisions.
 * @param minValue       Minimum numeric value (used for CHOOSE_X decisions).
 * @param maxValue       Maximum numeric value or selection count.
 * @param timeoutSeconds Seconds until auto-pass is applied.
 */
public record DecisionRequestDTO(
        UUID requestId,
        UUID seatId,
        String kind,
        String prompt,
        List<String> options,
        int minValue,
        int maxValue,
        long timeoutSeconds
) {
    /**
     * Convenience factory for an auto-pass (no-op) decision request.
     * Used internally when the engine transitions priority automatically.
     */
    public static DecisionRequestDTO autoPass() {
        return new DecisionRequestDTO(
                UUID.randomUUID(),
                null,
                "PASS_PRIORITY",
                "Auto-pass",
                List.of(),
                0,
                0,
                0L
        );
    }
}
