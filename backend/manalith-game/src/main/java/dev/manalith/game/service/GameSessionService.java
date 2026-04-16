package dev.manalith.game.service;

import dev.manalith.forge_adapter.ClientDecision;
import dev.manalith.forge_adapter.ForgeGameAdapter;
import dev.manalith.forge_adapter.ForgeGameSession;
import dev.manalith.forge_adapter.ForgeGameSession.ForgeGameConfig;
import dev.manalith.forge_adapter.ForgeGameSession.ForgePlayerConfig;
import dev.manalith.forge_adapter.ForgeGameSession.ForgePlayerSeat;
import dev.manalith.forge_adapter.ForgeGameSession.GameSessionStatus;
import dev.manalith.forge_adapter.GameActionEnvelope;
import dev.manalith.forge_adapter.GameEventPublisher;
import dev.manalith.forge_adapter.GameOutcomeDTO;
import dev.manalith.forge_adapter.GameStateDTO;
import dev.manalith.forge_adapter.GameStateSerializer;
import dev.manalith.forge_adapter.NetworkPlayerController;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GameSessionService
 *
 * Orchestrates the full lifecycle of in-progress game sessions. This service
 * is the primary entry point for:
 *
 * <ul>
 *   <li>Starting a new game session via {@link #startGame}</li>
 *   <li>Routing inbound client actions to the correct
 *       {@link NetworkPlayerController} via {@link #handleAction}</li>
 *   <li>Serving game-state snapshots per player via {@link #getGameState}</li>
 * </ul>
 *
 * <p>Action routing is performed via a {@code switch} on
 * {@link GameActionEnvelope.ActionPayload#kind()}, dispatching to the
 * relevant controller method or directly terminating the session.
 *
 * <h2>Validation</h2>
 * <ol>
 *   <li>Session existence — throws {@link GameSessionNotFoundException} if the
 *       room has no active session.</li>
 *   <li>Seat ownership — throws {@link SeatOwnershipException} if the
 *       action's {@code seatId} is not in the session.</li>
 *   <li>Turn ownership (stub) — throws {@link NotYourTurnException} for actions
 *       that require priority; stub always allows through until Forge enforces it.</li>
 * </ol>
 *
 * @see ForgeGameAdapter
 * @see NetworkPlayerController
 * @see WebSocketGameEventPublisher
 */
@Service
public class GameSessionService {

    private final ForgeGameAdapter forgeGameAdapter;
    private final GameEventPublisher gameEventPublisher;
    private final ApplicationEventPublisher appEventPublisher;
    private final GameStateSerializer gameStateSerializer;

    public GameSessionService(ForgeGameAdapter forgeGameAdapter,
                              GameEventPublisher gameEventPublisher,
                              ApplicationEventPublisher appEventPublisher,
                              GameStateSerializer gameStateSerializer) {
        this.forgeGameAdapter    = forgeGameAdapter;
        this.gameEventPublisher  = gameEventPublisher;
        this.appEventPublisher   = appEventPublisher;
        this.gameStateSerializer = gameStateSerializer;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Starts a new game session for the given room.
     *
     * <p>Creates a {@link ForgeGameSession} via {@link ForgeGameAdapter#createSession},
     * transitions it to {@link GameSessionStatus#RUNNING}, and broadcasts the
     * initial {@link GameStateDTO} to all players.
     *
     * @param roomId        Room UUID (from manalith-lobby).
     * @param playerConfigs One config per player.
     * @param config        Game format / timing config.
     * @return The live {@link ForgeGameSession}.
     * @throws IllegalStateException if a session already exists for this room.
     */
    public ForgeGameSession startGame(UUID roomId,
                                      List<ForgePlayerConfig> playerConfigs,
                                      ForgeGameConfig config) {
        ForgeGameSession session = forgeGameAdapter.createSession(roomId, playerConfigs, config);
        session.withStatus(GameSessionStatus.RUNNING);

        // Broadcast initial game state to all connected seats
        for (ForgePlayerSeat seat : session.seats()) {
            GameStateDTO state = gameStateSerializer.serializeForPlayer(null, seat.seatId());
            gameEventPublisher.emitGameState(roomId, state);
        }

        return session;
    }

    // ─── Action routing ───────────────────────────────────────────────────────

    /**
     * Validates an inbound client action and routes it to the correct controller.
     *
     * <p>Validation steps (in order):
     * <ol>
     *   <li>Session must exist for {@code roomId}.</li>
     *   <li>Seat must belong to the session.</li>
     *   <li>Turn/priority ownership check (stub — always passes).</li>
     * </ol>
     *
     * <p>Routing via {@code action.payload().kind()}:
     * <ul>
     *   <li>{@code PASS_PRIORITY}   → {@link NetworkPlayerController#handlePassPriority()}</li>
     *   <li>{@code CAST_SPELL}      → push {@link ClientDecision} into controller inbox</li>
     *   <li>{@code PLAY_LAND}       → push {@link ClientDecision} into controller inbox</li>
     *   <li>{@code ACTIVATE_ABILITY}→ push {@link ClientDecision} into controller inbox</li>
     *   <li>{@code CHOOSE_TARGETS}  → push into controller inbox</li>
     *   <li>{@code CHOOSE_X}        → push into controller inbox</li>
     *   <li>{@code DECLARE_ATTACKERS} → push into controller inbox</li>
     *   <li>{@code DECLARE_BLOCKERS}  → push into controller inbox</li>
     *   <li>{@code MULLIGAN_DECISION} → push into controller inbox</li>
     *   <li>{@code CONCEDE}         → terminate session with reason {@code "CONCEDE"}</li>
     *   <li>{@code CHAT}            → no-op (handled by lobby service)</li>
     * </ul>
     *
     * @param roomId  The room the action targets.
     * @param seatId  The seat the action originates from.
     * @param action  The decoded action envelope.
     * @throws GameSessionNotFoundException if no session exists for this room.
     * @throws SeatOwnershipException       if seatId is not in the session.
     */
    public void handleAction(UUID roomId, UUID seatId, GameActionEnvelope action) {
        ForgeGameSession session = requireSession(roomId);
        ForgePlayerSeat seat     = requireSeat(session, seatId);

        // Stub: in Phase 2, validate it is actually this player's turn / priority
        validateTurnOwnership(session, seatId, action);

        NetworkPlayerController controller = seat.controller();
        if (controller == null) {
            gameEventPublisher.emitActionRejected(roomId, seatId,
                    "Controller not initialised; player may not be connected yet.");
            return;
        }

        String kind = action.payload() != null ? action.payload().kind() : "UNKNOWN";

        switch (kind) {
            case "PASS_PRIORITY" -> controller.handlePassPriority();

            case "CAST_SPELL" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "PLAY_LAND" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "ACTIVATE_ABILITY" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "CHOOSE_TARGETS" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "CHOOSE_X" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "DECLARE_ATTACKERS" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "DECLARE_BLOCKERS" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "MULLIGAN_DECISION" -> {
                ClientDecision d = buildClientDecision(action, seatId);
                controller.receiveDecision(d);
            }

            case "CONCEDE" -> {
                // Immediately terminate the session; no need to route to controller
                forgeGameAdapter.terminateSession(roomId);
                GameOutcomeDTO outcome = GameOutcomeDTO.placeholder(session.gameId(), roomId);
                gameEventPublisher.emitGameOver(roomId, outcome);
            }

            case "SIDEBOARD_SUBMIT" -> {
                // No-op for now; sideboard handling is a Phase 2 concern
                gameEventPublisher.emitActionRejected(roomId, seatId,
                        "SIDEBOARD_SUBMIT not yet implemented.");
            }

            case "CHAT" -> {
                // Chat is handled by the lobby/notifications module, not here
            }

            default -> gameEventPublisher.emitActionRejected(roomId, seatId,
                    "Unknown action kind: " + kind);
        }
    }

    // ─── State queries ────────────────────────────────────────────────────────

    /**
     * Returns the current game state for the given perspective player.
     *
     * @param roomId              The room UUID.
     * @param perspectivePlayerId The seatId of the requesting player.
     * @return An {@link Optional} containing the game-state DTO, or empty if no
     *         session exists.
     */
    public Optional<GameStateDTO> getGameState(UUID roomId, UUID perspectivePlayerId) {
        return forgeGameAdapter.getSession(roomId)
                .map(session -> gameStateSerializer.serializeForPlayer(null, perspectivePlayerId));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ForgeGameSession requireSession(UUID roomId) {
        return forgeGameAdapter.getSession(roomId)
                .orElseThrow(() -> new GameSessionNotFoundException(
                        "No active session for room: " + roomId));
    }

    private ForgePlayerSeat requireSeat(ForgeGameSession session, UUID seatId) {
        ForgePlayerSeat seat = session.findSeat(seatId);
        if (seat == null) {
            throw new SeatOwnershipException(
                    "Seat " + seatId + " is not in session " + session.roomId());
        }
        return seat;
    }

    /**
     * Stub turn-ownership check. When Forge is integrated, this should verify
     * that the given seatId currently holds priority in the Forge game engine.
     *
     * <p>TODO (Phase 2): Query {@code forge.game.Game#getPlayerPriorityIndex()} or
     * similar to enforce priority ordering.
     */
    private void validateTurnOwnership(ForgeGameSession session,
                                        UUID seatId,
                                        GameActionEnvelope action) {
        // Stub: all actions pass the ownership check
        // TODO: implement real priority/turn check once Forge is integrated
        String kind = action.payload() != null ? action.payload().kind() : "";
        // CONCEDE and CHAT are always allowed regardless of turn
        if ("CONCEDE".equals(kind) || "CHAT".equals(kind)) {
            return;
        }
        // For all other kinds: future implementation will check Forge priority
    }

    /**
     * Converts a {@link GameActionEnvelope} into a {@link ClientDecision}
     * suitable for enqueueing into a {@link NetworkPlayerController}.
     */
    private ClientDecision buildClientDecision(GameActionEnvelope action, UUID seatId) {
        GameActionEnvelope.ActionPayload payload = action.payload();
        return new ClientDecision(
                action.actionId(),
                action.requestId(),
                seatId,
                payload != null ? payload.kind() : "UNKNOWN",
                payload != null ? payload.objectRef() : null,
                payload != null && payload.targetIds() != null ? payload.targetIds() : List.of(),
                payload != null ? payload.intValue() : 0,
                payload != null ? payload.raw() : null
        );
    }

    // ─── Exception types ──────────────────────────────────────────────────────

    /** Thrown when a client action targets a room with no active game session. */
    public static class GameSessionNotFoundException extends RuntimeException {
        public GameSessionNotFoundException(String message) { super(message); }
    }

    /** Thrown when a client action claims a seat that doesn't exist in the session. */
    public static class SeatOwnershipException extends RuntimeException {
        public SeatOwnershipException(String message) { super(message); }
    }

    /** Thrown when a client action requires priority but it's not that player's turn. */
    public static class NotYourTurnException extends RuntimeException {
        public NotYourTurnException(String message) { super(message); }
    }
}
