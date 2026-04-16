package dev.manalith.forge_adapter;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForgeGameAdapter
 *
 * Spring service responsible for the full lifecycle of Forge game instances.
 * It bridges Manalith's room/session model with the Forge rules engine,
 * translating {@link ForgeGameSession.ForgePlayerConfig} and
 * {@link ForgeGameSession.ForgeGameConfig} objects into running Forge matches.
 *
 * <h2>Forge Instantiation (once forge-game is on classpath)</h2>
 *
 * <ol>
 *   <li>A {@code GameRules} object is constructed with the appropriate
 *       {@code GameType} (Constructed, Limited, Commander, etc.).</li>
 *   <li>Each {@code ForgePlayerConfig} is converted to a {@code RegisteredPlayer}
 *       by parsing the raw decklist with Forge's {@code DeckSerializer}.</li>
 *   <li>A {@code Match} is created and {@code match.createGame()} returns the
 *       live {@code Game} instance.</li>
 *   <li>Each Forge {@code Player} receives a {@link NetworkPlayerController} that
 *       routes all decision callbacks over WebSocket.</li>
 *   <li>The match runs on a dedicated daemon thread; the adapter holds a reference
 *       to the {@code Game} inside the {@link ForgeGameSession} for state reads.</li>
 * </ol>
 *
 * <p>Sessions are stored in {@link #activeSessions} keyed by room UUID.
 * A session is removed from the map when {@link #terminateSession(UUID)} is called
 * (e.g. on concede, game-over, or server shutdown).
 *
 * <p>Spring {@link ApplicationEvent}s are published for external listeners:
 * {@link GameSessionCreatedEvent} on creation and {@link GameSessionTerminatedEvent}
 * on removal.
 *
 * @see ForgeGameSession
 * @see NetworkPlayerController
 * @see <a href="https://github.com/Card-Forge/forge">Forge on GitHub</a>
 */
@Service
public class ForgeGameAdapter {

    /** Live sessions keyed by roomId. */
    private final ConcurrentHashMap<UUID, ForgeGameSession> activeSessions =
            new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    public ForgeGameAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    // ─── Session management ───────────────────────────────────────────────────

    /**
     * Creates and registers a new game session for the given room.
     *
     * <p>A {@link NetworkPlayerController} is created for every player config and
     * bound to a {@link ForgeGameSession.ForgePlayerSeat}. The session starts in
     * {@link ForgeGameSession.GameSessionStatus#WAITING WAITING} status until all
     * players' WebSocket connections are confirmed.
     *
     * <p>Once Forge is on the classpath, this method should additionally:
     * <ul>
     *   <li>Parse decks with {@code DeckSerializer}</li>
     *   <li>Build a {@code Match} and call {@code match.createGame()}</li>
     *   <li>Assign controllers to Forge {@code Player} objects</li>
     *   <li>Spin up a daemon thread calling {@code match.startMatch()}</li>
     * </ul>
     *
     * @param roomId        The unique room UUID (from manalith-lobby).
     * @param playerConfigs One config per participating player.
     * @param gameConfig    Format and timing configuration for this game.
     * @return The newly created (and registered) {@link ForgeGameSession}.
     * @throws IllegalStateException if a session for this roomId already exists.
     */
    public ForgeGameSession createSession(UUID roomId,
                                          List<ForgeGameSession.ForgePlayerConfig> playerConfigs,
                                          ForgeGameSession.ForgeGameConfig gameConfig) {
        if (activeSessions.containsKey(roomId)) {
            throw new IllegalStateException(
                    "Session already exists for room: " + roomId);
        }

        // TODO: When forge-game is on classpath, replace stub with:
        // GameRules rules = new GameRules(GameType.Constructed);
        // List<RegisteredPlayer> registered = playerConfigs.stream()
        //     .map(pc -> { RegisteredPlayer rp = new RegisteredPlayer(pc.deck()); ... return rp; })
        //     .toList();
        // Match match = new Match(rules, registered, "Manalith Game");
        // Game game = match.createGame();
        // game.getPlayers().forEach(p -> p.setController(new NetworkPlayerController(...)));
        // new Thread(() -> match.startMatch()).start();

        UUID gameId = UUID.randomUUID();

        // Build a seat + controller for each player config
        List<ForgeGameSession.ForgePlayerSeat> seats = playerConfigs.stream()
                .map(pc -> {
                    UUID seatId = UUID.randomUUID();
                    NetworkPlayerController controller =
                            new NetworkPlayerController(null, roomId, seatId);
                    // NOTE: publisher will be wired in later via
                    // WebSocketGameEventPublisher after the session is registered
                    return new ForgeGameSession.ForgePlayerSeat(
                            seatId,
                            pc.userId(),
                            pc.displayName(),
                            controller
                    );
                })
                .toList();

        ForgeGameSession session = new ForgeGameSession(
                roomId,
                gameId,
                gameConfig,
                seats,
                ForgeGameSession.GameSessionStatus.WAITING,
                Instant.now()
        );

        activeSessions.put(roomId, session);
        eventPublisher.publishEvent(new GameSessionCreatedEvent(this, session));
        return session;
    }

    /**
     * Retrieves an active session by room ID.
     *
     * @param roomId The room UUID.
     * @return An {@link Optional} containing the session, or empty if not found.
     */
    public Optional<ForgeGameSession> getSession(UUID roomId) {
        return Optional.ofNullable(activeSessions.get(roomId));
    }

    /**
     * Terminates and removes a game session.
     *
     * <p>Once Forge is on the classpath, this should also call
     * {@code game.getMatch().endCurrentGame()} or interrupt the match thread.
     *
     * @param roomId The room UUID of the session to terminate.
     */
    public void terminateSession(UUID roomId) {
        ForgeGameSession session = activeSessions.remove(roomId);
        if (session != null) {
            session.withStatus(ForgeGameSession.GameSessionStatus.COMPLETED);
            eventPublisher.publishEvent(new GameSessionTerminatedEvent(this, session));
        }
    }

    // ─── Spring Application Events ────────────────────────────────────────────

    /**
     * Published when a new {@link ForgeGameSession} is successfully created.
     *
     * <p>Spring 5+ supports arbitrary POJO events (no need to extend
     * {@code ApplicationEvent}). Listeners may use this to send initial
     * {@code GAME_STATE} snapshots, start match-history records, or
     * transition the lobby room state.
     *
     * @param source  The {@link ForgeGameAdapter} that created the session.
     * @param session The newly created session.
     */
    public record GameSessionCreatedEvent(
            Object source,
            ForgeGameSession session
    ) {}

    /**
     * Published when a {@link ForgeGameSession} is removed from the active map.
     *
     * <p>Spring 5+ supports arbitrary POJO events. Listeners may use this to
     * persist match results, clean up WebSocket session registrations, notify
     * lobby, or release Forge resources.
     *
     * @param source  The {@link ForgeGameAdapter} that terminated the session.
     * @param session The terminated session (status will be COMPLETED or ERROR).
     */
    public record GameSessionTerminatedEvent(
            Object source,
            ForgeGameSession session
    ) {}
}
