package dev.manalith.deck.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.deck.dto.DeckDetailDTO;
import dev.manalith.deck.dto.DeckEntryDTO;
import dev.manalith.deck.exception.DeckImportException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Exports a deck to various text formats: plain text, MTGO .dec, Arena, and JSON.
 */
@Service
@RequiredArgsConstructor
public class DeckExportService {

    private final DeckService deckService;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Load the deck and export it in the requested format.
     *
     * @param deckId      UUID of the deck to export.
     * @param requesterId UUID of the requesting user (used to enforce visibility).
     * @param exportFormat One of {@code "text"}, {@code "mtgo"}, {@code "arena"}, {@code "json"}.
     * @return The deck as a formatted string.
     */
    public String export(UUID deckId, UUID requesterId, String exportFormat) {
        DeckDetailDTO deck = deckService.getDeckDetail(deckId, requesterId);
        return switch (exportFormat == null ? "text" : exportFormat.toLowerCase()) {
            case "mtgo"  -> exportAsMtgo(deck);
            case "arena" -> exportAsArena(deck);
            case "json"  -> exportAsJson(deck);
            default      -> exportAsText(deck);
        };
    }

    // -------------------------------------------------------------------------
    // Format implementations
    // -------------------------------------------------------------------------

    /**
     * Plain-text export:
     * <pre>
     * // Deck Name
     * // Format: Standard
     *
     * 4 Lightning Bolt
     * ...
     *
     * Sideboard:
     * 2 Pyroblast
     * </pre>
     */
    String exportAsText(DeckDetailDTO deck) {
        StringBuilder sb = new StringBuilder();
        sb.append("// ").append(deck.name()).append('\n');
        if (deck.format() != null) {
            sb.append("// Format: ").append(deck.format()).append('\n');
        }
        sb.append('\n');

        List<DeckEntryDTO> mainboard = safeList(deck.mainboard());
        for (DeckEntryDTO entry : mainboard) {
            sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
        }

        List<DeckEntryDTO> sideboard = safeList(deck.sideboard());
        if (!sideboard.isEmpty()) {
            sb.append('\n').append("Sideboard:\n");
            for (DeckEntryDTO entry : sideboard) {
                sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * MTGO .dec export:
     * <pre>
     * // Deck Name
     * 4 Lightning Bolt
     * 20 Mountain
     *
     * 2 Pyroblast
     * </pre>
     */
    String exportAsMtgo(DeckDetailDTO deck) {
        StringBuilder sb = new StringBuilder();
        sb.append("// ").append(deck.name()).append('\n');

        List<DeckEntryDTO> mainboard = safeList(deck.mainboard());
        for (DeckEntryDTO entry : mainboard) {
            sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
        }

        List<DeckEntryDTO> sideboard = safeList(deck.sideboard());
        if (!sideboard.isEmpty()) {
            sb.append('\n');
            for (DeckEntryDTO entry : sideboard) {
                sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * MTG Arena export:
     * <pre>
     * Commander          ← only for Commander format
     * 1 Atraxa, ...
     *
     * Deck
     * 4 Lightning Bolt
     *
     * Sideboard
     * 2 Pyroblast
     * </pre>
     */
    String exportAsArena(DeckDetailDTO deck) {
        StringBuilder sb = new StringBuilder();
        boolean isCommander = "commander".equalsIgnoreCase(deck.format());

        // Commander section — cards in the commanders list
        if (isCommander) {
            List<DeckEntryDTO> commanders = safeList(deck.commanders());
            if (!commanders.isEmpty()) {
                sb.append("Commander\n");
                for (DeckEntryDTO entry : commanders) {
                    sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
                }
                sb.append('\n');
            }
        }

        // Main deck
        sb.append("Deck\n");
        List<DeckEntryDTO> mainboard = safeList(deck.mainboard());
        for (DeckEntryDTO entry : mainboard) {
            sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
        }

        // Sideboard
        List<DeckEntryDTO> sideboard = safeList(deck.sideboard());
        if (!sideboard.isEmpty()) {
            sb.append('\n').append("Sideboard\n");
            for (DeckEntryDTO entry : sideboard) {
                sb.append(entry.quantity()).append(' ').append(entry.cardName()).append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * JSON export — serialises the full {@link DeckDetailDTO} via Jackson.
     */
    String exportAsJson(DeckDetailDTO deck) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deck);
        } catch (JsonProcessingException e) {
            throw new DeckImportException("Failed to serialise deck as JSON: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<DeckEntryDTO> safeList(List<DeckEntryDTO> list) {
        return list == null ? List.of() : list;
    }
}
