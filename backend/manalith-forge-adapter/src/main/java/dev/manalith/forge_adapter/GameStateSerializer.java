package dev.manalith.forge_adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * GameStateSerializer
 *
 * Spring component responsible for converting Forge's internal game-object
 * graph into the wire DTOs ({@link GameStateDTO}, {@link GameDiffDTO}) used by
 * the Manalith WebSocket protocol.
 *
 * <h2>Forge Object Mapping (when forge-game is on classpath)</h2>
 *
 * <p>The Forge view model is a parallel, read-only snapshot of the game state
 * that exists specifically for this kind of serialization:
 *
 * <ul>
 *   <li>{@code forge.game.GameView} — top-level view containing all sub-views;
 *       created via {@code new GameView(game, player)} where {@code player} is the
 *       perspective player (null for a spectator/full view).</li>
 *   <li>{@code forge.game.player.PlayerView} — per-player view obtained from
 *       {@code GameView.getPlayers()}, containing life, hand (with hidden info
 *       applied), library size, graveyard, exile, and mana pool.</li>
 *   <li>{@code forge.game.card.CardView} — individual card view from
 *       {@code PlayerView.getHand()}, {@code PlayerView.getGraveyard()},
 *       or {@code GameView.getBattlefield()}. Has {@code isKnown()} which returns
 *       {@code false} for opponent's hidden cards.</li>
 *   <li>{@code forge.game.zone.ZoneType} — enum for HAND, LIBRARY, GRAVEYARD,
 *       BATTLEFIELD, EXILE, STACK, COMMAND.</li>
 *   <li>{@code forge.game.spellability.SpellAbilityStackInstance} — stack item
 *       view accessible via {@code GameView.getStack()}.</li>
 * </ul>
 *
 * <h2>Hidden Information Policy</h2>
 *
 * <p>The {@link VisibilityPolicy} enum controls which cards are fully revealed:
 * <ul>
 *   <li>{@link VisibilityPolicy#PLAYER_SCOPED} — default; opponent hand cards
 *       are sent as {@code {isKnown: false, name: "Unknown Card"}}.</li>
 *   <li>{@link VisibilityPolicy#FULL_VISIBILITY} — all cards revealed (used for
 *       server-side logging, replays, and admin tooling).</li>
 *   <li>{@link VisibilityPolicy#SPECTATOR} — same as player-scoped but no
 *       hidden info for any player (spectators see nothing hidden).</li>
 * </ul>
 *
 * <p>TODO (Phase 2): Implement real Forge view model traversal once forge-game
 * is on the classpath. Stub methods currently return minimal placeholder DTOs.
 *
 * @see GameStateDTO
 * @see GameDiffDTO
 * @see <a href="https://github.com/Card-Forge/forge">Forge on GitHub</a>
 */
@Component
public class GameStateSerializer {

    /**
     * Converts a Forge {@code Game} object (passed as {@code Object} to avoid
     * compile-time dependency on Forge) into a full game-state snapshot for the
     * specified perspective player.
     *
     * <p>Hidden information is applied based on {@link VisibilityPolicy#PLAYER_SCOPED}:
     * opponent hand cards are masked.
     *
     * <p>TODO (Forge): Cast {@code forgeGame} to {@code forge.game.Game} and build
     * a {@code forge.game.GameView} for the perspective player:
     * <pre>
     * // forge.game.Game game = (forge.game.Game) forgeGame;
     * // forge.game.player.Player perspPlayer = findPlayerBySeatId(game, perspectivePlayerId);
     * // forge.game.GameView view = new forge.game.GameView(game, perspPlayer);
     * // ... map view to GameStateDTO ...
     * </pre>
     *
     * @param forgeGame           The live Forge {@code Game} instance (typed as {@code Object}).
     * @param perspectivePlayerId The seatId of the player this snapshot is for.
     * @return A {@link GameStateDTO} with hidden information applied.
     */
    public GameStateDTO serializeForPlayer(Object forgeGame, UUID perspectivePlayerId) {
        // STUB: return a minimal placeholder until Forge is integrated
        // TODO: Replace with real Forge GameView traversal (see Javadoc above)
        return GameStateDTO.placeholder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                perspectivePlayerId
        );
    }

    /**
     * Produces an incremental diff between two Forge game states.
     *
     * <p>The diff is computed by taking two successive state snapshots and
     * comparing them field-by-field, emitting {@link GameDiffDTO.DiffEntry}
     * records for every change.
     *
     * <p>TODO (Forge): Cast both parameters to {@code forge.game.GameView} and
     * perform a structural comparison:
     * <pre>
     * // forge.game.GameView prev = (forge.game.GameView) prevState;
     * // forge.game.GameView next = (forge.game.GameView) newState;
     * // List&lt;DiffEntry&gt; entries = new ArrayList&lt;&gt;();
     * // comparePlayerViews(prev.getPlayers(), next.getPlayers(), entries);
     * // compareBattlefield(prev.getBattlefield(), next.getBattlefield(), entries);
     * // compareStack(prev.getStack(), next.getStack(), entries);
     * // return new GameDiffDTO(roomId, prev.getRevision(), next.getRevision(), entries);
     * </pre>
     *
     * @param prevState The previous Forge game state (typed as {@code Object}).
     * @param newState  The new Forge game state (typed as {@code Object}).
     * @return A {@link GameDiffDTO} listing all changes between the two states.
     */
    public GameDiffDTO serializeDiff(Object prevState, Object newState) {
        // STUB: return an empty diff until Forge is integrated
        // TODO: Replace with real GameView structural comparison (see Javadoc above)
        return GameDiffDTO.empty(UUID.randomUUID(), 0L);
    }

    /**
     * Returns the full, unmasked game state for server-side use (logging, replays).
     * Uses {@link VisibilityPolicy#FULL_VISIBILITY}.
     *
     * @param forgeGame The live Forge {@code Game} instance.
     * @return A {@link GameStateDTO} with no hidden information applied.
     */
    public GameStateDTO serializeFullVisibility(Object forgeGame) {
        // STUB: return placeholder
        // TODO: Build GameView with null perspective player to get full visibility
        return GameStateDTO.placeholder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Visibility Policy
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Controls which cards are fully revealed when serializing game state.
     *
     * <p>Forge's {@code GameView} already applies player-scoped visibility when
     * constructed with a specific perspective player. These enum values map to
     * how the {@code GameView} is constructed:
     *
     * <ul>
     *   <li>{@link #FULL_VISIBILITY} → {@code new GameView(game, null)}</li>
     *   <li>{@link #PLAYER_SCOPED}   → {@code new GameView(game, perspectivePlayer)}</li>
     *   <li>{@link #SPECTATOR}       → same as PLAYER_SCOPED but spectator flag set</li>
     * </ul>
     */
    public enum VisibilityPolicy {

        /**
         * All cards are revealed regardless of zone or controller.
         * Used for server-side logging, replay recording, and admin tooling.
         */
        FULL_VISIBILITY,

        /**
         * Standard player view: opponent hand cards and face-down permanents
         * are masked. Library cards are never sent, only the count.
         * Used for the primary game WebSocket channel.
         */
        PLAYER_SCOPED,

        /**
         * Spectator view: no hidden information is revealed for any player.
         * All hands appear as unknown cards; face-down permanents are anonymous.
         * Library counts are visible.
         */
        SPECTATOR
    }
}
