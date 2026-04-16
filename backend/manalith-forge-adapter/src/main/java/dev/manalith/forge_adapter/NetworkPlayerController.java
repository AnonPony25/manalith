package dev.manalith.forge_adapter;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * NetworkPlayerController
 *
 * Implements Forge's PlayerController abstract class for a remote human player
 * connected via WebSocket. Each decision point in Forge calls one of the
 * override methods here; this class:
 *
 *  1. Emits a DecisionRequest event to the client via GameEventPublisher.
 *  2. Blocks on {@code inbox} until the client sends a matching ClientDecision.
 *  3. Validates and applies the decision, then returns control to Forge.
 *
 * NOTE: This is a stub. Full Forge integration requires adding forge-game
 * as a dependency and extending forge.game.player.PlayerController.
 *
 * @see <a href="https://github.com/Card-Forge/forge">Forge on GitHub</a>
 */
public class NetworkPlayerController /* extends PlayerController */ {

    private final BlockingQueue<ClientDecision> inbox = new LinkedBlockingQueue<>();
    private final GameEventPublisher publisher;
    private final UUID roomId;
    private final UUID seatId;

    /** Timeout (seconds) before auto-pass is applied. */
    private static final long DECISION_TIMEOUT_SECONDS = 45L;

    public NetworkPlayerController(GameEventPublisher publisher,
                                   UUID roomId,
                                   UUID seatId) {
        this.publisher = publisher;
        this.roomId    = roomId;
        this.seatId    = seatId;
    }

    /**
     * Called by Forge when this player has priority and may act.
     * Emits a PRIORITY prompt; client may pass or take an action.
     */
    public void awaitNextInput() {
        publisher.emitPriorityPrompt(roomId, seatId);
    }

    /**
     * Enqueues a decision received from the client WebSocket handler.
     * Thread-safe; called from the WebSocket I/O thread.
     */
    public void receiveDecision(ClientDecision decision) {
        inbox.offer(decision);
    }

    /**
     * Blocks until the client sends a decision or the timeout elapses.
     * On timeout, returns an auto-pass decision.
     */
    protected ClientDecision takeExpectedDecision(String context) {
        try {
            ClientDecision d = inbox.poll(DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (d == null) {
                publisher.emitTimeoutWarning(roomId, seatId, context);
                return ClientDecision.autoPass();
            }
            return d;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ClientDecision.autoPass();
        }
    }

    // TODO: Override forge.game.player.PlayerController abstract methods:
    //   chooseTargetsFor(), declareAttackers(), declareBlockers(),
    //   chooseNumber(), chooseMana(), mulliganDecision(), etc.
    //   Each should emit a typed DecisionRequest and call takeExpectedDecision().
}
