package dev.manalith.game.service;

import dev.manalith.forge_adapter.ClientDecision;
import dev.manalith.forge_adapter.ForgeGameAdapter;
import dev.manalith.forge_adapter.ForgeGameSession;
import dev.manalith.forge_adapter.ForgeGameSession.ForgeGameConfig;
import dev.manalith.forge_adapter.ForgeGameSession.ForgePlayerConfig;
import dev.manalith.forge_adapter.ForgeGameSession.ForgePlayerSeat;
import dev.manalith.forge_adapter.GameActionEnvelope;
import dev.manalith.forge_adapter.GameEventPublisher;
import dev.manalith.forge_adapter.GameStateDTO;
import dev.manalith.forge_adapter.GameStateSerializer;
import dev.manalith.forge_adapter.NetworkPlayerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GameSessionService}.
 *
 * <p>All Forge dependencies are absent from the classpath; tests rely only on
 * the public stub API of the adapter layer, mocked via Mockito.
 */
@ExtendWith(MockitoExtension.class)
class GameSessionServiceTest {

    @Mock
    private ForgeGameAdapter forgeGameAdapter;

    @Mock
    private GameEventPublisher gameEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GameStateSerializer gameStateSerializer;

    private GameSessionService service;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final UUID ROOM_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GAME_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEAT_A   = UUID.fromString("00000000-0000-0000-0000-0000000000AA");
    private static final UUID SEAT_B   = UUID.fromString("00000000-0000-0000-0000-0000000000BB");
    private static final UUID USER_A   = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID USER_B   = UUID.fromString("00000000-0000-0000-0000-000000000011");

    private static final ForgeGameConfig DEFAULT_CONFIG =
            new ForgeGameConfig("Constructed", false, 2, 45);

    private static final List<ForgePlayerConfig> TWO_PLAYERS = List.of(
            new ForgePlayerConfig(USER_A, "Alice", "4 Mountain\n56 other cards\n"),
            new ForgePlayerConfig(USER_B, "Bob",   "4 Island\n56 other cards\n")
    );

    @BeforeEach
    void setUp() {
        service = new GameSessionService(
                forgeGameAdapter,
                gameEventPublisher,
                applicationEventPublisher,
                gameStateSerializer
        );
    }

    // ─── Helper: build a ForgeGameSession with real seats ─────────────────────

    private ForgeGameSession buildRunningSession() {
        NetworkPlayerController controllerA = spy(
                new NetworkPlayerController(gameEventPublisher, ROOM_ID, SEAT_A));
        NetworkPlayerController controllerB = spy(
                new NetworkPlayerController(gameEventPublisher, ROOM_ID, SEAT_B));

        ForgePlayerSeat seatA = new ForgePlayerSeat(SEAT_A, USER_A, "Alice", controllerA);
        ForgePlayerSeat seatB = new ForgePlayerSeat(SEAT_B, USER_B, "Bob",   controllerB);

        return new ForgeGameSession(
                ROOM_ID,
                GAME_ID,
                DEFAULT_CONFIG,
                List.of(seatA, seatB),
                ForgeGameSession.GameSessionStatus.RUNNING,
                Instant.now()
        );
    }

    // ─── startGame ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startGame should call ForgeGameAdapter.createSession and emit game state")
    void startGame_createsSessionAndPublishesEvent() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.createSession(eq(ROOM_ID), eq(TWO_PLAYERS), eq(DEFAULT_CONFIG)))
                .thenReturn(session);
        when(gameStateSerializer.serializeForPlayer(any(), any()))
                .thenReturn(GameStateDTO.placeholder(GAME_ID, ROOM_ID, SEAT_A));

        ForgeGameSession result = service.startGame(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        assertThat(result).isNotNull();
        assertThat(result.roomId()).isEqualTo(ROOM_ID);

        // Verify adapter was called
        verify(forgeGameAdapter).createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        // Verify game state was emitted for each seat (2 players)
        verify(gameEventPublisher, org.mockito.Mockito.times(2))
                .emitGameState(eq(ROOM_ID), any(GameStateDTO.class));
    }

    // ─── handleAction ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("handleAction with PASS_PRIORITY and valid seat should route to controller")
    void handleAction_withValidSeat_routesToController() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, SEAT_A,
                UUID.randomUUID(), null,
                new GameActionEnvelope.ActionPayload("PASS_PRIORITY", null, List.of(), 0, null)
        );

        // Should not throw
        service.handleAction(ROOM_ID, SEAT_A, action);

        // Verify adapter was queried
        verify(forgeGameAdapter).getSession(ROOM_ID);
    }

    @Test
    @DisplayName("handleAction with unknown seatId should throw SeatOwnershipException")
    void handleAction_withWrongSeat_throwsException() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        UUID unknownSeat = UUID.fromString("99999999-0000-0000-0000-000000000099");
        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, unknownSeat,
                UUID.randomUUID(), null,
                new GameActionEnvelope.ActionPayload("PASS_PRIORITY", null, List.of(), 0, null)
        );

        assertThatThrownBy(() -> service.handleAction(ROOM_ID, unknownSeat, action))
                .isInstanceOf(GameSessionService.SeatOwnershipException.class)
                .hasMessageContaining(unknownSeat.toString());
    }

    @Test
    @DisplayName("handleAction when session does not exist should throw GameSessionNotFoundException")
    void handleAction_sessionNotFound_throwsException() {
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.empty());

        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, SEAT_A,
                UUID.randomUUID(), null,
                new GameActionEnvelope.ActionPayload("PASS_PRIORITY", null, List.of(), 0, null)
        );

        assertThatThrownBy(() -> service.handleAction(ROOM_ID, SEAT_A, action))
                .isInstanceOf(GameSessionService.GameSessionNotFoundException.class)
                .hasMessageContaining(ROOM_ID.toString());
    }

    @Test
    @DisplayName("handleAction with CAST_SPELL should push decision into controller inbox")
    void handleAction_castSpell_pushesDecisionToInbox() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        String cardRef = "card-uuid-abc123";
        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, SEAT_A,
                UUID.randomUUID(), UUID.randomUUID(),
                new GameActionEnvelope.ActionPayload(
                        "CAST_SPELL", cardRef, List.of(), 0, "{\"kind\":\"CAST_SPELL\"}")
        );

        service.handleAction(ROOM_ID, SEAT_A, action);

        // Verify the controller for seat A received a decision
        NetworkPlayerController controllerA = session.findSeat(SEAT_A).controller();
        // The inbox should now have one item
        ClientDecision polled = controllerA.takeExpectedDecision("test", 0L);
        assertThat(polled).isNotNull();
        assertThat(polled.kind()).isEqualTo("CAST_SPELL");
        assertThat(polled.objectRef()).isEqualTo(cardRef);
    }

    @Test
    @DisplayName("handleAction with CONCEDE should terminate session and emit game over")
    void handleAction_concede_terminatesSession() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, SEAT_A,
                UUID.randomUUID(), null,
                new GameActionEnvelope.ActionPayload("CONCEDE", null, List.of(), 0, null)
        );

        service.handleAction(ROOM_ID, SEAT_A, action);

        verify(forgeGameAdapter).terminateSession(ROOM_ID);
        verify(gameEventPublisher).emitGameOver(eq(ROOM_ID), any());
    }

    @Test
    @DisplayName("handleAction with unknown kind should emit ACTION_REJECTED")
    void handleAction_unknownKind_emitsRejected() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        GameActionEnvelope action = new GameActionEnvelope(
                "GAME_ACTION", 1, ROOM_ID, GAME_ID, SEAT_A,
                UUID.randomUUID(), null,
                new GameActionEnvelope.ActionPayload("TOTALLY_FAKE_KIND", null, List.of(), 0, null)
        );

        service.handleAction(ROOM_ID, SEAT_A, action);

        verify(gameEventPublisher).emitActionRejected(eq(ROOM_ID), eq(SEAT_A), any());
    }

    // ─── getGameState ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getGameState returns state when session exists")
    void getGameState_sessionExists_returnsState() {
        ForgeGameSession session = buildRunningSession();
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.of(session));

        GameStateDTO expectedDto = GameStateDTO.placeholder(GAME_ID, ROOM_ID, SEAT_A);
        when(gameStateSerializer.serializeForPlayer(any(), eq(SEAT_A))).thenReturn(expectedDto);

        Optional<GameStateDTO> result = service.getGameState(ROOM_ID, SEAT_A);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedDto);
    }

    @Test
    @DisplayName("getGameState returns empty when session does not exist")
    void getGameState_sessionAbsent_returnsEmpty() {
        when(forgeGameAdapter.getSession(ROOM_ID)).thenReturn(Optional.empty());

        Optional<GameStateDTO> result = service.getGameState(ROOM_ID, SEAT_A);

        assertThat(result).isEmpty();
        verify(gameStateSerializer, never()).serializeForPlayer(any(), any());
    }
}
