package dev.manalith.forge_adapter;

import java.util.List;
import java.util.UUID;

/**
 * GameStateDTO
 *
 * Full game-state snapshot sent to a connected player as the {@code GAME_STATE}
 * event defined in {@code docs/PROTOCOL.md}.
 *
 * <p>Each field represents the subset of Forge's internal game model that is
 * safe and relevant to send to a specific player (hidden-information rules apply).
 * See {@code docs/PROTOCOL.md#Hidden Information Rules} for details.
 *
 * <p>When Forge is on the classpath, these fields are populated by
 * {@link GameStateSerializer#serializeForPlayer(Object, UUID)} by reading from
 * Forge's {@code forge.game.GameView}, {@code forge.game.player.PlayerView},
 * and {@code forge.game.card.CardView} objects.
 *
 * TODO: Expand fields in Phase 2 based on the full Forge view model:
 * <ul>
 *   <li>Each {@code CardView} maps to a {@code CardStateDTO} with:
 *       name, manaCost, typeLine, oracleText, power, toughness, isKnown</li>
 *   <li>Each {@code PlayerView} maps to a {@code PlayerStateDTO} with:
 *       life, handSize (or cards if self), libraryCount, graveyardCards,
 *       exileCards, commandZoneCards, manaPool</li>
 *   <li>The battlefield maps to a list of {@code PermanentDTO} per player</li>
 *   <li>The stack maps to a list of {@code StackItemDTO}</li>
 * </ul>
 *
 * @param gameId          The unique game UUID.
 * @param roomId          The room UUID this game belongs to.
 * @param revision        Monotonically increasing state version (for diff tracking).
 * @param activePlayerSeatId The seatId of the player who currently has priority.
 * @param turnNumber      Current turn number (starts at 1).
 * @param phase           Current phase/step (e.g. "MAIN1", "COMBAT_ATTACKERS").
 * @param players         Per-player state snapshots (possibly with hidden info).
 * @param stack           Current contents of the stack (top-of-stack first).
 * @param battlefield     All permanents on the battlefield, grouped by controller seatId.
 * @param perspective     The seatId this state is scoped to (for hidden-info filtering).
 */
public record GameStateDTO(
        UUID gameId,
        UUID roomId,
        long revision,
        UUID activePlayerSeatId,
        int turnNumber,
        String phase,
        List<PlayerStateDTO> players,
        List<StackItemDTO> stack,
        List<PermanentDTO> battlefield,
        UUID perspective
) {

    /**
     * Minimal placeholder snapshot used by stub methods before Forge integration.
     * All non-UUID fields are set to sensible zero-values.
     */
    public static GameStateDTO placeholder(UUID gameId, UUID roomId, UUID perspective) {
        return new GameStateDTO(
                gameId,
                roomId,
                0L,
                null,
                0,
                "UNKNOWN",
                List.of(),
                List.of(),
                List.of(),
                perspective
        );
    }

    // ─── Nested DTOs ──────────────────────────────────────────────────────────

    /**
     * Per-player state visible to the perspective player.
     * Cards in hand are hidden for opponents (isKnown=false, name="Unknown Card").
     */
    public record PlayerStateDTO(
            UUID seatId,
            String displayName,
            int life,
            int handCount,
            int libraryCount,
            boolean isActive,
            boolean hasPriority
    ) {}

    /**
     * An item currently on the stack (spell or ability).
     * Corresponds to Forge's {@code forge.game.spellability.SpellAbilityStackInstance}.
     */
    public record StackItemDTO(
            String stackId,
            String description,
            UUID controllerSeatId
    ) {}

    /**
     * A permanent on the battlefield.
     * Corresponds to Forge's {@code forge.game.card.CardView} for a battlefield card.
     */
    public record PermanentDTO(
            String objectRef,
            String name,
            String typeLine,
            UUID controllerSeatId,
            boolean isTapped,
            boolean isSummoningSick,
            int power,
            int toughness
    ) {}
}
