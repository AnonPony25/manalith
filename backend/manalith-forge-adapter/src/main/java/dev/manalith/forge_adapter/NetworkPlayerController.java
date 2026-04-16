package dev.manalith.forge_adapter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * NetworkPlayerController
 *
 * Implements Forge's {@code PlayerController} abstract class for a remote human
 * player connected via WebSocket. Each decision point in Forge calls one of the
 * override methods here; this class:
 *
 * <ol>
 *   <li>Emits a typed {@link DecisionRequestDTO} event to the client via
 *       {@link GameEventPublisher}, including a unique {@code requestId}.</li>
 *   <li>Blocks on {@link #inbox} until the client sends a matching
 *       {@link ClientDecision} or the configurable timeout elapses.</li>
 *   <li>Validates and returns the decision to the Forge rules engine.</li>
 * </ol>
 *
 * <h2>Full Forge Integration</h2>
 * Once {@code forge-game} is on the classpath, remove the comment markers below
 * and restore:
 * <pre>
 * // TODO (Forge): public class NetworkPlayerController extends
 * //     forge.game.player.PlayerController { ... }
 * </pre>
 * Then override the abstract methods: {@code chooseTargetsFor()},
 * {@code declareAttackers()}, {@code declareBlockers()}, {@code chooseNumber()},
 * {@code chooseMana()}, {@code mulliganDecision()}, and all remaining abstract
 * methods in {@code forge.game.player.PlayerController}.
 *
 * <p>Each override should follow the same pattern as the methods below:
 * build a {@link DecisionRequestDTO}, publish it, then call
 * {@link #takeExpectedDecision(String, long)}.
 *
 * @see <a href="https://github.com/Card-Forge/forge">Forge on GitHub</a>
 * @see GameEventPublisher
 * @see ClientDecision
 */
// TODO (Forge): extends forge.game.player.PlayerController
public class NetworkPlayerController {

    /** Decision messages enqueued from the WebSocket receive thread. */
    private final BlockingQueue<ClientDecision> inbox = new LinkedBlockingQueue<>();

    private final GameEventPublisher publisher;
    private final UUID roomId;
    private final UUID seatId;

    /** Default timeout in seconds before an auto-pass is applied. */
    private static final long DEFAULT_DECISION_TIMEOUT_SECONDS = 45L;

    /**
     * Constructs a controller bound to a specific seat in a specific room.
     *
     * @param publisher GameEventPublisher used to emit decision requests to the client.
     * @param roomId    The room this game is hosted in.
     * @param seatId    The seat/player this controller manages.
     */
    public NetworkPlayerController(GameEventPublisher publisher,
                                   UUID roomId,
                                   UUID seatId) {
        this.publisher = publisher;
        this.roomId    = roomId;
        this.seatId    = seatId;
    }

    // ─── Inbox management ─────────────────────────────────────────────────────

    /**
     * Enqueues a decision received from the client WebSocket handler.
     * Thread-safe; called from the WebSocket I/O thread.
     *
     * @param decision The decoded decision from the client's {@code GAME_ACTION} message.
     */
    public void receiveDecision(ClientDecision decision) {
        inbox.offer(decision);
    }

    /**
     * Blocks until the client sends a decision or the timeout elapses.
     *
     * <p>On timeout, a {@code DECISION_TIMEOUT_WARNING} event is emitted and an
     * auto-pass decision is returned so the Forge rules engine can continue.
     *
     * @param context        Human-readable label for this decision point (for logs/UI).
     * @param timeoutSeconds Seconds to wait before auto-pass fires.
     * @return The client's {@link ClientDecision}, or {@link ClientDecision#autoPass()}
     *         if the timeout elapses.
     */
    public ClientDecision takeExpectedDecision(String context, long timeoutSeconds) {
        try {
            ClientDecision d = inbox.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (d == null) {
                if (publisher != null) {
                    publisher.emitTimeoutWarning(roomId, seatId, context);
                }
                return ClientDecision.autoPass();
            }
            return d;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ClientDecision.autoPass();
        }
    }

    /**
     * Overload using the default timeout of {@value #DEFAULT_DECISION_TIMEOUT_SECONDS}s.
     *
     * @param context Human-readable label for this decision point.
     * @return The client's {@link ClientDecision} or an auto-pass on timeout.
     */
    public ClientDecision takeExpectedDecision(String context) {
        return takeExpectedDecision(context, DEFAULT_DECISION_TIMEOUT_SECONDS);
    }

    // ─── Priority ─────────────────────────────────────────────────────────────

    /**
     * Called when this player has priority and may act.
     * Emits a {@code PRIORITY_CHANGED} prompt to the client; client may pass
     * priority or perform an action (cast spell, activate ability, etc.).
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#chooseAction(Game, Player, SpellAbility)}.
     */
    public void handlePassPriority() {
        UUID requestId = UUID.randomUUID();
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "PASS_PRIORITY",
                "You have priority. Pass or take an action.",
                List.of(),
                0,
                0,
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
            publisher.emitPriorityPrompt(roomId, seatId);
        }
        takeExpectedDecision("PASS_PRIORITY");
    }

    // ─── Targeting ────────────────────────────────────────────────────────────

    /**
     * Called by Forge when a spell or ability requires the player to choose targets.
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#chooseTargetsFor(SpellAbility)}.
     *
     * @param abilityDescription Human-readable description of the effect needing targets.
     * @param validTargetIds     Object-ref IDs of legal targets (permanent IDs, player IDs).
     */
    public void handleChooseTargets(String abilityDescription, List<String> validTargetIds) {
        UUID requestId = UUID.randomUUID();
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "CHOOSE_TARGETS",
                abilityDescription,
                validTargetIds,
                0,
                validTargetIds.size(),
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
        }
        takeExpectedDecision("CHOOSE_TARGETS[" + abilityDescription + "]");
    }

    // ─── Combat: Attackers ─────────────────────────────────────────────────────

    /**
     * Called by Forge during the declare-attackers step.
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#declareAttackers(Player, Combat)}.
     *
     * @param attackerIds Object-ref IDs of creatures that may attack.
     */
    public void handleDeclareAttackers(List<String> attackerIds) {
        UUID requestId = UUID.randomUUID();
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "DECLARE_ATTACKERS",
                "Declare attackers",
                attackerIds,
                0,
                attackerIds.size(),
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
        }
        takeExpectedDecision("DECLARE_ATTACKERS");
    }

    // ─── Combat: Blockers ─────────────────────────────────────────────────────

    /**
     * Called by Forge during the declare-blockers step.
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#declareBlockers(Player, Combat)}.
     *
     * @param blockerIds  Object-ref IDs of creatures that may block.
     * @param attackerIds Object-ref IDs of attacking creatures.
     */
    public void handleDeclareBlockers(List<String> blockerIds, List<String> attackerIds) {
        UUID requestId = UUID.randomUUID();
        List<String> combined = new java.util.ArrayList<>(blockerIds);
        combined.addAll(attackerIds);
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "DECLARE_BLOCKERS",
                "Declare blockers",
                combined,
                0,
                blockerIds.size(),
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
        }
        takeExpectedDecision("DECLARE_BLOCKERS");
    }

    // ─── X / Number choices ───────────────────────────────────────────────────

    /**
     * Called by Forge when a spell or ability requires the player to choose a number
     * (e.g. the value of X for an X-spell).
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#chooseNumber(SpellAbility, String, int, int)}.
     *
     * @param prompt  Descriptive prompt shown to the player.
     * @param min     Minimum legal value.
     * @param max     Maximum legal value.
     */
    public void handleChooseNumber(String prompt, int min, int max) {
        UUID requestId = UUID.randomUUID();
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "CHOOSE_X",
                prompt,
                List.of(),
                min,
                max,
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
        }
        takeExpectedDecision("CHOOSE_X[" + prompt + "]");
    }

    // ─── Mulligan ─────────────────────────────────────────────────────────────

    /**
     * Called by Forge at the start of the game (or after a previous mulligan)
     * to ask whether the player wants to keep or take a mulligan.
     *
     * <p>TODO (Forge): Override
     * {@code forge.game.player.PlayerController#mulliganDecision(Player, int)}.
     *
     * @param handSize The number of cards currently in the player's opening hand.
     */
    public void handleMulliganDecision(int handSize) {
        UUID requestId = UUID.randomUUID();
        DecisionRequestDTO req = new DecisionRequestDTO(
                requestId,
                seatId,
                "MULLIGAN_DECISION",
                "Keep " + handSize + "-card hand or take a mulligan?",
                List.of("KEEP", "MULLIGAN"),
                0,
                0,
                DEFAULT_DECISION_TIMEOUT_SECONDS
        );
        if (publisher != null) {
            publisher.emitDecisionRequest(roomId, seatId, req);
        }
        takeExpectedDecision("MULLIGAN_DECISION[handSize=" + handSize + "]");
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public UUID getRoomId() { return roomId; }
    public UUID getSeatId() { return seatId; }
}
