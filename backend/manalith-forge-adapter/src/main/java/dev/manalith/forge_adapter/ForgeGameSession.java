package dev.manalith.forge_adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ForgeGameSession
 *
 * Represents a single running game instance managed by the Forge rules engine.
 * One session corresponds to one in-progress game room. The session encapsulates
 * all mutable state needed to route WebSocket actions to the correct Forge
 * player controller and to serialize state snapshots for broadcast.
 *
 * <p>Thread-safety: {@code status} is the only mutable field; callers should
 * update it via {@link #withStatus(GameSessionStatus)} to produce a new instance,
 * or rely on {@link ForgeGameAdapter} which manages the registry under locks.
 *
 * <p>When Forge is on the classpath, a reference to the live
 * {@code forge.game.Game} object should be added here alongside the existing
 * seats. Serialization is delegated to {@link GameStateSerializer}.
 */
public final class ForgeGameSession {

    private final UUID roomId;
    private final UUID gameId;
    private final ForgeGameConfig config;
    private final List<ForgePlayerSeat> seats;
    private volatile GameSessionStatus status;
    private final Instant startedAt;
    private volatile Instant endedAt;

    /**
     * Primary constructor. Called by {@link ForgeGameAdapter#createSession}.
     */
    public ForgeGameSession(UUID roomId,
                            UUID gameId,
                            ForgeGameConfig config,
                            List<ForgePlayerSeat> seats,
                            GameSessionStatus status,
                            Instant startedAt) {
        this.roomId    = roomId;
        this.gameId    = gameId;
        this.config    = config;
        this.seats     = List.copyOf(seats);
        this.status    = status;
        this.startedAt = startedAt;
        this.endedAt   = null;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public UUID roomId()    { return roomId; }
    public UUID gameId()    { return gameId; }
    public ForgeGameConfig config()   { return config; }
    public List<ForgePlayerSeat> seats() { return seats; }
    public GameSessionStatus status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant endedAt()   { return endedAt; }

    /** Returns a copy of the seat matching the given seatId, or null if absent. */
    public ForgePlayerSeat findSeat(UUID seatId) {
        return seats.stream()
                .filter(s -> s.seatId().equals(seatId))
                .findFirst()
                .orElse(null);
    }

    /** Returns a copy of the seat matching the given userId, or null if absent. */
    public ForgePlayerSeat findSeatByUser(UUID userId) {
        return seats.stream()
                .filter(s -> s.userId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    // ─── Mutators (return new instances for immutability) ─────────────────────

    /** Transition to a new status. If COMPLETED or ERROR, stamps endedAt. */
    public ForgeGameSession withStatus(GameSessionStatus newStatus) {
        this.status = newStatus;
        if (newStatus == GameSessionStatus.COMPLETED || newStatus == GameSessionStatus.ERROR) {
            this.endedAt = Instant.now();
        }
        return this;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Enum: GameSessionStatus
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Lifecycle states of a {@link ForgeGameSession}.
     *
     * <ul>
     *   <li>{@link #WAITING}   – Session created; waiting for all players to connect.</li>
     *   <li>{@link #RUNNING}   – All players connected; Forge match thread is active.</li>
     *   <li>{@link #COMPLETED} – Game ended naturally (win/loss/draw).</li>
     *   <li>{@link #ERROR}     – Forge threw an unrecoverable exception; session is dead.</li>
     * </ul>
     */
    public enum GameSessionStatus {
        WAITING,
        RUNNING,
        COMPLETED,
        ERROR
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Record: ForgePlayerSeat
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Binds a human player's identity to their seat in a game.
     *
     * <p>The {@code controller} may be {@code null} until the player's WebSocket
     * connection is established and registered by
     * {@link dev.manalith.game.service.WebSocketGameEventPublisher}.
     *
     * @param seatId      Unique seat identifier (stable across reconnects).
     * @param userId      The user's platform UUID.
     * @param displayName The user's display name shown in-game.
     * @param controller  The live {@link NetworkPlayerController}, or {@code null}
     *                    when the player has not yet connected.
     */
    public record ForgePlayerSeat(
            UUID seatId,
            UUID userId,
            String displayName,
            NetworkPlayerController controller
    ) {}

    // ──────────────────────────────────────────────────────────────────────────
    // Record: ForgeGameConfig
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Immutable configuration that governs how a Forge game is initialised.
     *
     * <p>When Forge is on the classpath, {@code format} is used to select the
     * appropriate {@code forge.game.GameRules} and {@code forge.game.GameType}:
     * <pre>
     * // TODO: GameType gameType = GameType.valueOf(config.format().toUpperCase());
     * // TODO: GameRules rules = new GameRules(gameType);
     * // TODO: rules.setPlayForAnte(false);
     * </pre>
     *
     * @param format                The game format name (e.g. "Constructed", "Limited").
     * @param isCommander           Whether Commander/EDH rules apply.
     * @param maxPlayers            Maximum number of players (2 for 1v1, 4 for Commander).
     * @param actionTimeoutSeconds  Seconds before auto-pass fires per decision.
     */
    public record ForgeGameConfig(
            String format,
            boolean isCommander,
            int maxPlayers,
            int actionTimeoutSeconds
    ) {}

    // ──────────────────────────────────────────────────────────────────────────
    // Record: ForgePlayerConfig
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Per-player configuration submitted when creating a game session.
     *
     * <p>The {@code deckList} is a raw MTGO-style decklist (one "N CardName" per
     * line). It will be parsed by Forge's {@code DeckSerializer} once Forge is
     * on the classpath:
     * <pre>
     * // TODO: Deck deck = DeckSerializer.fromSections(DeckFileHeader.parse(pc.deckList()));
     * // TODO: RegisteredPlayer rp = new RegisteredPlayer(deck).setPlayer(player);
     * </pre>
     *
     * @param userId      The user's platform UUID.
     * @param displayName The user's display name.
     * @param deckList    Raw MTGO-format decklist text.
     */
    public record ForgePlayerConfig(
            UUID userId,
            String displayName,
            String deckList
    ) {}

    // ──────────────────────────────────────────────────────────────────────────
    // Object helpers
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ForgeGameSession{roomId=" + roomId +
               ", gameId=" + gameId +
               ", status=" + status +
               ", players=" + seats.size() + "}";
    }
}
