package dev.manalith.deck.service;

import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.deck.dto.CreateDeckRequest;
import dev.manalith.deck.dto.DeckDetailDTO;
import dev.manalith.deck.dto.DeckSummaryDTO;
import dev.manalith.deck.exception.DeckImportException;
import dev.manalith.deck.service.DeckImportService.ParsedEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeckImportService}.
 *
 * <p>Uses Mockito only — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class DeckImportServiceTest {

    @Mock
    private CardPrintingRepository cardPrintingRepository;

    @Mock
    private DeckService deckService;

    private DeckImportService importService;

    private UUID ownerId;
    private UUID deckId;

    @BeforeEach
    void setUp() {
        importService = new DeckImportService(cardPrintingRepository, deckService);
        ownerId = UUID.randomUUID();
        deckId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // parsePlainText tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parsePlainText: basic deck parses quantities and names")
    void parsePlainText_basicDeck_parsesQuantitiesAndNames() {
        String deckList = """
                4 Lightning Bolt
                4x Counterspell
                20 Mountain
                """;

        List<ParsedEntry> entries = importService.parsePlainText(deckList);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).cardName()).isEqualTo("Lightning Bolt");
        assertThat(entries.get(0).quantity()).isEqualTo(4);
        assertThat(entries.get(0).isSideboard()).isFalse();

        assertThat(entries.get(1).cardName()).isEqualTo("Counterspell");
        assertThat(entries.get(1).quantity()).isEqualTo(4);

        assertThat(entries.get(2).cardName()).isEqualTo("Mountain");
        assertThat(entries.get(2).quantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("parsePlainText: lines after 'Sideboard' marker are marked as sideboard")
    void parsePlainText_sideboardSection_marksEntriesAsSideboard() {
        String deckList = """
                4 Lightning Bolt
                Sideboard
                2 Pyroblast
                1 Red Elemental Blast
                """;

        List<ParsedEntry> entries = importService.parsePlainText(deckList);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).isSideboard()).isFalse();
        assertThat(entries.get(1).isSideboard()).isTrue();
        assertThat(entries.get(2).isSideboard()).isTrue();
    }

    @Test
    @DisplayName("parsePlainText: comment lines starting with '//' or '#' are skipped")
    void parsePlainText_commentsSkipped() {
        String deckList = """
                // This is a comment
                # This too
                4 Lightning Bolt
                // Another comment
                2 Mountain
                """;

        List<ParsedEntry> entries = importService.parsePlainText(deckList);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).cardName()).isEqualTo("Lightning Bolt");
        assertThat(entries.get(1).cardName()).isEqualTo("Mountain");
    }

    // -------------------------------------------------------------------------
    // parseMtgo tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parseMtgo: lines with set/collector info parse card name only")
    void parseMtgo_withSetInfo_parsesNameOnly() {
        String deckList = """
                // My MTGO Deck
                4 Lightning Bolt (M10) 146
                2 Counterspell (ICE) 54
                """;

        List<ParsedEntry> entries = importService.parseMtgo(deckList);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).cardName()).isEqualTo("Lightning Bolt");
        assertThat(entries.get(0).quantity()).isEqualTo(4);
        assertThat(entries.get(0).isSideboard()).isFalse();

        assertThat(entries.get(1).cardName()).isEqualTo("Counterspell");
    }

    @Test
    @DisplayName("parseMtgo: second block after blank line is the sideboard")
    void parseMtgo_twoBlocks_secondIsSideboard() {
        String deckList = """
                4 Lightning Bolt
                20 Mountain
                
                2 Pyroblast
                1 Red Elemental Blast
                """;

        List<ParsedEntry> entries = importService.parseMtgo(deckList);

        assertThat(entries).hasSize(4);
        assertThat(entries.get(0).isSideboard()).isFalse();
        assertThat(entries.get(1).isSideboard()).isFalse();
        assertThat(entries.get(2).isSideboard()).isTrue();
        assertThat(entries.get(3).isSideboard()).isTrue();
    }

    // -------------------------------------------------------------------------
    // parseArena tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parseArena: section headers set correct modes for entries")
    void parseArena_withSectionHeaders_parsesCorrectly() {
        String deckList = """
                Deck
                4 Lightning Bolt
                20 Mountain
                
                Sideboard
                2 Pyroblast
                """;

        List<ParsedEntry> entries = importService.parseArena(deckList);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).isSideboard()).isFalse();
        assertThat(entries.get(0).isCommander()).isFalse();
        assertThat(entries.get(2).isSideboard()).isTrue();
    }

    @Test
    @DisplayName("parseArena: Commander section entries have isCommander=true")
    void parseArena_commanderSection_setsIsCommander() {
        String deckList = """
                Commander
                1 Atraxa, Praetors' Voice
                
                Deck
                4 Path to Exile
                """;

        List<ParsedEntry> entries = importService.parseArena(deckList);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).cardName()).isEqualTo("Atraxa, Praetors' Voice");
        assertThat(entries.get(0).isCommander()).isTrue();
        assertThat(entries.get(1).isCommander()).isFalse();
    }

    // -------------------------------------------------------------------------
    // importFromParsed — warning / threshold tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("importDeck: unresolved cards are logged as WARN but import continues under 20% threshold")
    void importDeck_unresolvedCards_logsWarning() {
        // 1 resolved, 1 unresolved → 50% unresolved → should throw
        ParsedEntry resolved = new ParsedEntry("Lightning Bolt", 4, false, false);
        ParsedEntry unresolved = new ParsedEntry("Nonexistent Card XYZ", 1, false, false);

        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(UUID.randomUUID());

        when(cardPrintingRepository.findFirstByName("Lightning Bolt")).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findFirstByName("Nonexistent Card XYZ")).thenReturn(Optional.empty());
        when(cardPrintingRepository.findFirstByNameLikeIgnoreCase("%Nonexistent Card XYZ%")).thenReturn(Optional.empty());

        DeckSummaryDTO summaryDTO = new DeckSummaryDTO(
                deckId.toString(), ownerId.toString(), null,
                "Test", "standard", null, false, 0, 0, null, null);
        DeckDetailDTO detailDTO = new DeckDetailDTO(
                deckId.toString(), ownerId.toString(), null,
                "Test", "standard", null, false, null, null,
                List.of(), List.of(), List.of(), null);

        when(deckService.createDeck(eq(ownerId), any(CreateDeckRequest.class))).thenReturn(summaryDTO);

        // 50% unresolved → DeckImportException expected
        assertThatThrownBy(() ->
                importService.importFromParsed(ownerId, "Test", "standard", List.of(resolved, unresolved))
        ).isInstanceOf(DeckImportException.class)
                .hasMessageContaining("Too many unresolved cards");
    }

    @Test
    @DisplayName("importDeck: under 20% unresolved cards does not throw")
    void importDeck_fewUnresolvedCards_doesNotThrow() {
        // 9 resolved, 1 unresolved → 10% → should NOT throw
        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(UUID.randomUUID());

        when(cardPrintingRepository.findFirstByName("Lightning Bolt")).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findFirstByName("Unresolvable")).thenReturn(Optional.empty());
        when(cardPrintingRepository.findFirstByNameLikeIgnoreCase("%Unresolvable%")).thenReturn(Optional.empty());

        List<ParsedEntry> entries = List.of(
                new ParsedEntry("Lightning Bolt", 4, false, false),
                new ParsedEntry("Lightning Bolt", 4, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Lightning Bolt", 1, false, false),
                new ParsedEntry("Unresolvable", 1, false, false) // 10% unresolved
        );

        DeckSummaryDTO summaryDTO = new DeckSummaryDTO(
                deckId.toString(), ownerId.toString(), null,
                "Test", "standard", null, false, 0, 0, null, null);
        DeckDetailDTO detailDTO = new DeckDetailDTO(
                deckId.toString(), ownerId.toString(), null,
                "Test", "standard", null, false, null, null,
                List.of(), List.of(), List.of(), null);

        when(deckService.createDeck(eq(ownerId), any())).thenReturn(summaryDTO);
        when(deckService.addCard(any(), any(), any())).thenReturn(detailDTO);
        when(deckService.getDeckDetail(any(), any())).thenReturn(detailDTO);

        DeckDetailDTO result = importService.importFromParsed(ownerId, "Test", "standard", entries);
        assertThat(result).isNotNull();
    }
}
