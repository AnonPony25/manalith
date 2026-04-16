package dev.manalith.forge_adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * GameOutcomeDTO
 *
 * Payload for the {@code GAME_OVER} event sent to all connected players when
 * a game ends, as defined in {@code docs/PROTOCOL.md}.
 *
 * <p>Contains the final result including winner(s), loser(s), reason for
 * termination, and per-player statistics for match-history recording.
 *
 * @param gameId      The unique game UUID.
 * @param roomId      The room UUID.
 * @param reason      How the game ended (e.g. {@code "LIFE_TOTAL"}, {@code "CONCEDE"},
 *                    {@code "DRAW"}, {@code "TIMEOUT"}, {@code "ADMIN_TERMINATED"}).
 * @param winnerIds   SeatIds of players who won (may be empty for draws).
 * @param loserIds    SeatIds of players who lost (may be empty for draws).
 * @param playerStats Per-player end-of-game statistics.
 * @param endedAt     Timestamp when the game concluded.
 */
public record GameOutcomeDTO(
        UUID gameId,
        UUID roomId,
        String reason,
        List<UUID> winnerIds,
        List<UUID> loserIds,
        List<PlayerFinalStats> playerStats,
        Instant endedAt
) {

    /** Constructs a placeholder outcome used before Forge integration. */
    public static GameOutcomeDTO placeholder(UUID gameId, UUID roomId) {
        return new GameOutcomeDTO(
                gameId,
                roomId,
                "UNKNOWN",
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    // ─── Nested ───────────────────────────────────────────────────────────────

    /**
     * Final per-player statistics recorded at game end.
     *
     * @param seatId    The player's seat UUID.
     * @param userId    The player's platform UUID.
     * @param finalLife Life total at game end (may be 0 or negative for losers).
     * @param turnsPlayed Number of turns this player was the active player.
     * @param cardsDrawn  Total cards drawn during the game.
     */
    public record PlayerFinalStats(
            UUID seatId,
            UUID userId,
            int finalLife,
            int turnsPlayed,
            int cardsDrawn
    ) {}
}
