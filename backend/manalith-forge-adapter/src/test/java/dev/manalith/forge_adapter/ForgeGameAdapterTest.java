package dev.manalith.forge_adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ForgeGameAdapter}.
 *
 * <p>All tests run without Forge on the classpath — the adapter only requires
 * Spring's {@link ApplicationEventPublisher}, which is mocked here.
 */
@ExtendWith(MockitoExtension.class)
class ForgeGameAdapterTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ForgeGameAdapter adapter;

    // ─── Fixture data ─────────────────────────────────────────────────────────

    private static final UUID ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final ForgeGameSession.ForgeGameConfig DEFAULT_CONFIG =
            new ForgeGameSession.ForgeGameConfig("Constructed", false, 2, 45);

    private static final List<ForgeGameSession.ForgePlayerConfig> TWO_PLAYERS = List.of(
            new ForgeGameSession.ForgePlayerConfig(
                    UUID.fromString("00000000-0000-0000-0000-000000000010"),
                    "Alice",
                    "4 Mountain\n56 other cards\n"),
            new ForgeGameSession.ForgePlayerConfig(
                    UUID.fromString("00000000-0000-0000-0000-000000000011"),
                    "Bob",
                    "4 Island\n56 other cards\n")
    );

    @BeforeEach
    void setUp() {
        adapter = new ForgeGameAdapter(applicationEventPublisher);
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSession should register a new session and publish GameSessionCreatedEvent")
    void createSession_shouldRegisterNewSession() {
        // Act
        ForgeGameSession session = adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        // Assert: session is present with correct metadata
        assertThat(session).isNotNull();
        assertThat(session.roomId()).isEqualTo(ROOM_ID);
        assertThat(session.gameId()).isNotNull();
        assertThat(session.config()).isEqualTo(DEFAULT_CONFIG);
        assertThat(session.seats()).hasSize(2);
        assertThat(session.status())
                .isEqualTo(ForgeGameSession.GameSessionStatus.WAITING);
        assertThat(session.startedAt()).isNotNull();
        assertThat(session.endedAt()).isNull();

        // Assert: session is retrievable
        Optional<ForgeGameSession> retrieved = adapter.getSession(ROOM_ID);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().roomId()).isEqualTo(ROOM_ID);

        // Assert: event published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        Object event = eventCaptor.getValue();
        assertThat(event).isInstanceOf(ForgeGameAdapter.GameSessionCreatedEvent.class);
        ForgeGameAdapter.GameSessionCreatedEvent createdEvent =
                (ForgeGameAdapter.GameSessionCreatedEvent) event;
        assertThat(createdEvent.session().roomId()).isEqualTo(ROOM_ID);
    }

    @Test
    @DisplayName("createSession should throw IllegalStateException when session already exists")
    void createSession_whenDuplicate_shouldThrow() {
        adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        assertThatThrownBy(() -> adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(ROOM_ID.toString());
    }

    @Test
    @DisplayName("terminateSession should remove session and publish GameSessionTerminatedEvent")
    void terminateSession_shouldRemoveSession() {
        // Arrange: session must exist first
        adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        // Act
        adapter.terminateSession(ROOM_ID);

        // Assert: session no longer retrievable
        assertThat(adapter.getSession(ROOM_ID)).isEmpty();

        // Assert: two events published (created + terminated)
        verify(applicationEventPublisher, times(2)).publishEvent(any());

        // Specifically verify the terminated event
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> allEvents = eventCaptor.getAllValues();
        assertThat(allEvents)
                .anySatisfy(e -> assertThat(e)
                        .isInstanceOf(ForgeGameAdapter.GameSessionTerminatedEvent.class));

        ForgeGameAdapter.GameSessionTerminatedEvent terminatedEvent =
                (ForgeGameAdapter.GameSessionTerminatedEvent) allEvents.stream()
                        .filter(e -> e instanceof ForgeGameAdapter.GameSessionTerminatedEvent)
                        .findFirst().orElseThrow();
        assertThat(terminatedEvent.session().roomId()).isEqualTo(ROOM_ID);
    }

    @Test
    @DisplayName("terminateSession on unknown room should be a no-op")
    void terminateSession_unknownRoom_isNoOp() {
        UUID unknownRoom = UUID.randomUUID();

        // Should not throw
        adapter.terminateSession(unknownRoom);

        // No events published
        verify(applicationEventPublisher, times(0)).publishEvent(any());
    }

    @Test
    @DisplayName("getSession for unknown room should return empty Optional")
    void getSession_unknownRoom_returnsEmpty() {
        UUID unknownRoom = UUID.randomUUID();

        Optional<ForgeGameSession> result = adapter.getSession(unknownRoom);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("created session should have a controller per seat")
    void createSession_seatsShouldHaveControllers() {
        ForgeGameSession session = adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        for (ForgeGameSession.ForgePlayerSeat seat : session.seats()) {
            assertThat(seat.seatId()).isNotNull();
            assertThat(seat.userId()).isNotNull();
            assertThat(seat.displayName()).isNotBlank();
            assertThat(seat.controller()).isNotNull();
        }
    }

    @Test
    @DisplayName("created session seats should reflect player display names")
    void createSession_seatDisplayNamesShouldMatchConfigs() {
        ForgeGameSession session = adapter.createSession(ROOM_ID, TWO_PLAYERS, DEFAULT_CONFIG);

        List<String> displayNames = session.seats().stream()
                .map(ForgeGameSession.ForgePlayerSeat::displayName)
                .toList();

        assertThat(displayNames).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @DisplayName("multiple rooms can coexist as independent sessions")
    void multipleSessions_shouldCoexistIndependently() {
        UUID roomA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        UUID roomB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

        adapter.createSession(roomA, TWO_PLAYERS, DEFAULT_CONFIG);
        adapter.createSession(roomB, TWO_PLAYERS, DEFAULT_CONFIG);

        assertThat(adapter.getSession(roomA)).isPresent();
        assertThat(adapter.getSession(roomB)).isPresent();

        adapter.terminateSession(roomA);

        assertThat(adapter.getSession(roomA)).isEmpty();
        assertThat(adapter.getSession(roomB)).isPresent();
    }
}
