package dev.manalith.forge_adapter;

import java.util.UUID;

/**
 * GameActionEnvelope
 *
 * Deserialized top-level container for all inbound client WebSocket messages
 * on the {@code /ws/game} endpoint, corresponding to the {@code GAME_ACTION}
 * message type defined in {@code docs/PROTOCOL.md}.
 *
 * <p>Example JSON:
 * <pre>
 * {
 *   "type": "GAME_ACTION",
 *   "version": 1,
 *   "roomId": "uuid",
 *   "gameId": "uuid",
 *   "seatId": "uuid",
 *   "actionId": "client-generated-uuid",
 *   "requestId": "uuid-or-null",
 *   "payload": { "kind": "CAST_SPELL", "objectRef": "card-uuid", ... }
 * }
 * </pre>
 *
 * @param type      Always {@code "GAME_ACTION"} (used for discriminating envelopes).
 * @param version   Protocol version (currently {@code 1}).
 * @param roomId    The room UUID this action targets.
 * @param gameId    The active game UUID.
 * @param seatId    The seat this action originates from.
 * @param actionId  Client-generated UUID for deduplication / idempotency.
 * @param requestId Echoed requestId from the originating {@link DecisionRequestDTO},
 *                  or {@code null} for unsolicited actions (e.g. CONCEDE, CHAT).
 * @param payload   Typed action payload.
 */
public record GameActionEnvelope(
        String type,
        int version,
        UUID roomId,
        UUID gameId,
        UUID seatId,
        UUID actionId,
        UUID requestId,
        ActionPayload payload
) {

    // ─── Nested ───────────────────────────────────────────────────────────────

    /**
     * Typed action payload carried inside a {@link GameActionEnvelope}.
     *
     * <p>The {@code kind} field discriminates the action type and determines
     * which additional fields are meaningful. See {@code docs/PROTOCOL.md} for
     * the full list of supported kinds.
     *
     * @param kind      Action discriminator (e.g. {@code PASS_PRIORITY},
     *                  {@code CAST_SPELL}, {@code DECLARE_ATTACKERS}).
     * @param objectRef Primary card/permanent object reference (for CAST_SPELL,
     *                  PLAY_LAND, ACTIVATE_ABILITY, etc.).
     * @param targetIds Selected target object references (for CHOOSE_TARGETS, etc.).
     * @param intValue  Integer value for CHOOSE_X, PAY_MANA cost components, etc.
     * @param raw       Full raw JSON payload string for forward-compatibility.
     */
    public record ActionPayload(
            String kind,
            String objectRef,
            java.util.List<String> targetIds,
            int intValue,
            String raw
    ) {
        /** Returns {@code true} if this is a PASS_PRIORITY action. */
        public boolean isPassPriority()    { return "PASS_PRIORITY".equals(kind); }
        /** Returns {@code true} if this is a CAST_SPELL action. */
        public boolean isCastSpell()       { return "CAST_SPELL".equals(kind); }
        /** Returns {@code true} if this is a CONCEDE action. */
        public boolean isConcede()         { return "CONCEDE".equals(kind); }
        /** Returns {@code true} if this is a DECLARE_ATTACKERS action. */
        public boolean isDeclareAttackers(){ return "DECLARE_ATTACKERS".equals(kind); }
        /** Returns {@code true} if this is a DECLARE_BLOCKERS action. */
        public boolean isDeclareBlockers() { return "DECLARE_BLOCKERS".equals(kind); }
        /** Returns {@code true} if this is a MULLIGAN_DECISION action. */
        public boolean isMulliganDecision(){ return "MULLIGAN_DECISION".equals(kind); }
        /** Returns {@code true} if this is a CHOOSE_TARGETS action. */
        public boolean isChooseTargets()   { return "CHOOSE_TARGETS".equals(kind); }
    }
}
