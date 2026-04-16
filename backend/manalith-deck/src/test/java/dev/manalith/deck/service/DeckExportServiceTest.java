package dev.manalith.deck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.deck.dto.DeckDetailDTO;
import dev.manalith.deck.dto.DeckEntryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeckExportService}.
 *
 * <p>Uses Mockito only — no Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class DeckExportServiceTest {

    @Mock
    private DeckService deckService;

    private DeckExportService exportService;
    private ObjectMapper objectMapper;

    private UUID deckId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        exportService = new DeckExportService(deckService, objectMapper);
        deckId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DeckEntryDTO mainEntry(String name, int qty) {
        return new DeckEntryDTO(UUID.randomUUID().toString(), name, "{1}{R}", 2.0,
                "Instant", "M10", "common", null, qty, false);
    }

    private DeckEntryDTO commanderEntry(String name) {
        return new DeckEntryDTO(UUID.randomUUID().toString(), name, "{W}{U}{B}{G}", 5.0,
                "Legendary Creature — Phyrexian Praetor", "NPH", "mythic", null, 1, true);
    }

    private DeckDetailDTO buildDeck(String name, String format,
                                    List<DeckEntryDTO> mainboard,
                                    List<DeckEntryDTO> sideboard,
                                    List<DeckEntryDTO> commanders) {
        return new DeckDetailDTO(
                deckId.toString(), ownerId.toString(), "TestUser",
                name, format, "Test deck",
                true,
                "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z",
                mainboard, sideboard, commanders,
                null
        );
    }

    // -------------------------------------------------------------------------
    // exportAsText tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("exportAsText: output includes deck name comment and sideboard section")
    void exportAsText_includesSideboardSection() {
        DeckDetailDTO deck = buildDeck("My Deck", "standard",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(mainEntry("Pyroblast", 2)),
                List.of());

        String output = exportService.exportAsText(deck);

        assertThat(output).contains("// My Deck");
        assertThat(output).contains("// Format: standard");
        assertThat(output).contains("4 Lightning Bolt");
        assertThat(output).contains("Sideboard:");
        assertThat(output).contains("2 Pyroblast");
    }

    @Test
    @DisplayName("exportAsText: no sideboard section if deck has no sideboard entries")
    void exportAsText_noSideboard_omitsSideboardSection() {
        DeckDetailDTO deck = buildDeck("My Deck", "modern",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(),
                List.of());

        String output = exportService.exportAsText(deck);

        assertThat(output).doesNotContain("Sideboard:");
    }

    // -------------------------------------------------------------------------
    // exportAsMtgo tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("exportAsMtgo: two-block format with blank line separating sideboard")
    void exportAsMtgo_twoBlockFormat() {
        DeckDetailDTO deck = buildDeck("Burn", "legacy",
                List.of(mainEntry("Lightning Bolt", 4), mainEntry("Mountain", 16)),
                List.of(mainEntry("Pyroblast", 2)),
                List.of());

        String output = exportService.exportAsMtgo(deck);

        // Header comment
        assertThat(output).contains("// Burn");
        // Main block
        assertThat(output).contains("4 Lightning Bolt");
        assertThat(output).contains("16 Mountain");
        // Blank line then sideboard
        assertThat(output).contains("\n\n2 Pyroblast");
    }

    @Test
    @DisplayName("exportAsMtgo: no blank sideboard block when sideboard is empty")
    void exportAsMtgo_emptySideboard_noExtraBlankLine() {
        DeckDetailDTO deck = buildDeck("Burn", "modern",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(),
                List.of());

        String output = exportService.exportAsMtgo(deck);

        // Should NOT have two consecutive blank lines after mainboard
        assertThat(output).doesNotContain("\n\n\n");
    }

    // -------------------------------------------------------------------------
    // exportAsArena tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("exportAsArena: Commander format prepends Commander section")
    void exportAsArena_commanderFormat_prependsCommanderSection() {
        DeckEntryDTO commander = commanderEntry("Atraxa, Praetors' Voice");
        DeckDetailDTO deck = buildDeck("Atraxa EDH", "commander",
                List.of(mainEntry("Path to Exile", 4)),
                List.of(),
                List.of(commander));

        String output = exportService.exportAsArena(deck);

        assertThat(output).startsWith("Commander\n");
        assertThat(output).contains("1 Atraxa, Praetors' Voice");
        assertThat(output).contains("Deck\n");
        assertThat(output).contains("4 Path to Exile");
    }

    @Test
    @DisplayName("exportAsArena: non-Commander format has no Commander section")
    void exportAsArena_nonCommanderFormat_noCommanderSection() {
        DeckDetailDTO deck = buildDeck("Burn", "modern",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(),
                List.of());

        String output = exportService.exportAsArena(deck);

        assertThat(output).doesNotContain("Commander\n");
        assertThat(output).startsWith("Deck\n");
    }

    @Test
    @DisplayName("exportAsArena: sideboard entries appear under Sideboard header")
    void exportAsArena_withSideboard_includesSideboardSection() {
        DeckDetailDTO deck = buildDeck("Control", "standard",
                List.of(mainEntry("Counterspell", 4)),
                List.of(mainEntry("Negate", 2)),
                List.of());

        String output = exportService.exportAsArena(deck);

        assertThat(output).contains("Sideboard\n");
        assertThat(output).contains("2 Negate");
    }

    // -------------------------------------------------------------------------
    // exportAsJson tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("exportAsJson: returns valid JSON with deck name field")
    void exportAsJson_returnsValidJson() throws Exception {
        DeckDetailDTO deck = buildDeck("JSON Deck", "vintage",
                List.of(mainEntry("Black Lotus", 1)),
                List.of(),
                List.of());

        String json = exportService.exportAsJson(deck);

        // Must be parseable JSON
        assertThatCode(() -> objectMapper.readTree(json)).doesNotThrowAnyException();

        // Must include the deck name
        assertThat(objectMapper.readTree(json).get("name").asText()).isEqualTo("JSON Deck");
    }

    @Test
    @DisplayName("exportAsJson: serialises mainboard entries")
    void exportAsJson_includesMainboardEntries() throws Exception {
        DeckDetailDTO deck = buildDeck("Test", "modern",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(),
                List.of());

        String json = exportService.exportAsJson(deck);
        var root = objectMapper.readTree(json);

        assertThat(root.has("mainboard")).isTrue();
        assertThat(root.get("mainboard").isArray()).isTrue();
        assertThat(root.get("mainboard").get(0).get("cardName").asText()).isEqualTo("Lightning Bolt");
    }

    // -------------------------------------------------------------------------
    // export() routing tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("export(): routes 'arena' format to exportAsArena")
    void export_routesToCorrectFormatter() {
        DeckDetailDTO deck = buildDeck("Test", "standard",
                List.of(mainEntry("Lightning Bolt", 4)),
                List.of(), List.of());

        when(deckService.getDeckDetail(eq(deckId), eq(ownerId))).thenReturn(deck);

        String output = exportService.export(deckId, ownerId, "arena");

        assertThat(output).contains("Deck\n");
    }
}
