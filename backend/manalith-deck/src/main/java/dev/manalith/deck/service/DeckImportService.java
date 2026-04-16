package dev.manalith.deck.service;

import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.deck.dto.AddCardRequest;
import dev.manalith.deck.dto.CreateDeckRequest;
import dev.manalith.deck.dto.DeckDetailDTO;
import dev.manalith.deck.exception.DeckImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses and imports decks from various text formats (plain text, MTGO .dec, Arena).
 *
 * <p>Parsing is performed in two phases:
 * <ol>
 *   <li>Format-specific parsing → list of {@link ParsedEntry} objects.</li>
 *   <li>Card resolution via {@link CardPrintingRepository}, followed by deck
 *       creation and card insertion through {@link DeckService}.</li>
 * </ol>
 *
 * <p>If more than 20 % of entries cannot be resolved to a known card, a
 * {@link DeckImportException} is thrown to signal a likely malformed list.
 * Individual unresolved cards are logged at WARN level but do not abort the
 * import on their own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckImportService {

    private final CardPrintingRepository cardPrintingRepository;
    private final DeckService deckService;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse and import a deck.
     *
     * @param ownerId      UUID of the authenticated user who will own the deck.
     * @param deckName     Display name for the new deck.
     * @param format       MTG format string (e.g. "standard", "commander").
     * @param deckList     Raw text of the deck list.
     * @param importFormat One of {@code "text"}, {@code "mtgo"}, or {@code "arena"}.
     * @return The newly created {@link DeckDetailDTO}.
     */
    public DeckDetailDTO importDeck(UUID ownerId, String deckName, String format,
                                    String deckList, String importFormat) {
        List<ParsedEntry> entries;
        try {
            entries = switch (importFormat == null ? "text" : importFormat.toLowerCase(Locale.ROOT)) {
                case "mtgo" -> parseMtgo(deckList);
                case "arena" -> parseArena(deckList);
                default -> parsePlainText(deckList);
            };
        } catch (DeckImportException e) {
            throw e;
        } catch (Exception e) {
            throw new DeckImportException("Failed to parse deck list: " + e.getMessage(), e);
        }

        return importFromParsed(ownerId, deckName, format, entries);
    }

    // -------------------------------------------------------------------------
    // Format parsers
    // -------------------------------------------------------------------------

    /**
     * Parse a plain-text deck list.
     *
     * <p>Line format: {@code [qty] [card name]} or {@code [qty]x [card name]}.
     * Lines starting with {@code //} or {@code #} are comments and are skipped.
     * Blank lines act as section separators.
     * A line reading {@code Sideboard} or {@code SB:} toggles sideboard mode.
     */
    List<ParsedEntry> parsePlainText(String deckList) {
        List<ParsedEntry> entries = new ArrayList<>();
        boolean sideboard = false;

        for (String rawLine : splitLines(deckList)) {
            String line = rawLine.strip();

            // Skip comments
            if (line.startsWith("//") || line.startsWith("#")) {
                continue;
            }

            // Blank line — section separator (not a mode toggle on its own)
            if (line.isBlank()) {
                continue;
            }

            // Sideboard toggle
            if (line.equalsIgnoreCase("Sideboard") || line.equalsIgnoreCase("SB:")) {
                sideboard = true;
                continue;
            }

            ParsedEntry entry = parseQuantityAndName(line, sideboard, false);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Parse an MTGO {@code .dec}-style deck list.
     *
     * <p>Lines starting with {@code //} are comments.
     * The first non-comment block is the mainboard; a blank line separates it
     * from the sideboard block.
     * Line format: {@code 4 Lightning Bolt (M10) 146} — set/collector info is ignored.
     */
    List<ParsedEntry> parseMtgo(String deckList) {
        List<ParsedEntry> entries = new ArrayList<>();
        boolean sideboard = false;
        boolean seenFirstBlock = false;
        boolean inBlankGap = false;

        for (String rawLine : splitLines(deckList)) {
            String line = rawLine.strip();

            if (line.startsWith("//")) {
                continue;
            }

            if (line.isBlank()) {
                if (seenFirstBlock) {
                    inBlankGap = true;
                }
                continue;
            }

            // Transition from blank gap back into content → sideboard block
            if (inBlankGap) {
                sideboard = true;
                inBlankGap = false;
            }
            seenFirstBlock = true;

            ParsedEntry entry = parseQuantityAndName(line, sideboard, false);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Parse an MTG Arena-style deck list.
     *
     * <p>Section headers: {@code Deck}, {@code Sideboard}, {@code Commander}.
     * Line format: {@code 4 Lightning Bolt (M10) 146} or {@code 4 Lightning Bolt}.
     */
    List<ParsedEntry> parseArena(String deckList) {
        List<ParsedEntry> entries = new ArrayList<>();
        boolean sideboard = false;
        boolean commander = false;

        for (String rawLine : splitLines(deckList)) {
            String line = rawLine.strip();

            if (line.isBlank()) {
                continue;
            }

            switch (line) {
                case "Deck" -> { sideboard = false; commander = false; continue; }
                case "Sideboard" -> { sideboard = true; commander = false; continue; }
                case "Commander" -> { commander = true; sideboard = false; continue; }
                default -> { /* fall through to card parsing */ }
            }

            ParsedEntry entry = parseQuantityAndName(line, sideboard, commander);
            if (entry != null) {
                entries.add(entry);
            }
        }

        return entries;
    }

    // -------------------------------------------------------------------------
    // Card resolution & deck construction
    // -------------------------------------------------------------------------

    /**
     * Create a deck from a list of parsed entries.
     *
     * <p>Unresolved cards are logged at WARN. If more than 20 % of total entries
     * are unresolved a {@link DeckImportException} is thrown.
     */
    DeckDetailDTO importFromParsed(UUID ownerId, String deckName, String format,
                                   List<ParsedEntry> entries) {
        // Create the skeleton deck
        CreateDeckRequest createRequest = new CreateDeckRequest(deckName, format, null, false);
        var deckSummary = deckService.createDeck(ownerId, createRequest);
        UUID deckId = UUID.fromString(deckSummary.id());

        List<String> unresolved = new ArrayList<>();

        for (ParsedEntry entry : entries) {
            Optional<CardPrinting> cardOpt = resolveCard(entry.cardName());
            if (cardOpt.isEmpty()) {
                log.warn("Could not resolve card '{}' during import of deck '{}'", entry.cardName(), deckName);
                unresolved.add(entry.cardName());
                continue;
            }

            AddCardRequest addRequest = new AddCardRequest(
                    cardOpt.get().getId().toString(),
                    entry.quantity(),
                    entry.isCommander(),
                    entry.isSideboard()
            );
            deckService.addCard(deckId, ownerId, addRequest);
        }

        // Fail if too many cards were unresolved (> 20 %)
        if (!entries.isEmpty()) {
            double unresolvedRatio = (double) unresolved.size() / entries.size();
            if (unresolvedRatio > 0.20) {
                throw new DeckImportException(
                        "Too many unresolved cards: " + String.join(", ", unresolved));
            }
        }

        return deckService.getDeckDetail(deckId, ownerId);
    }

    /**
     * Resolve a card name to a {@link CardPrinting}.
     * Tries exact match first; falls back to case-insensitive substring match.
     */
    Optional<CardPrinting> resolveCard(String name) {
        Optional<CardPrinting> exact = cardPrintingRepository.findFirstByName(name);
        if (exact.isPresent()) {
            return exact;
        }
        return cardPrintingRepository.findFirstByNameLikeIgnoreCase("%" + name + "%");
    }

    // -------------------------------------------------------------------------
    // Line-level parsing helpers
    // -------------------------------------------------------------------------

    /**
     * Parse a quantity + card name from a generic text line.
     * Supports {@code "4 Lightning Bolt"} and {@code "4x Lightning Bolt"}.
     * Strips trailing Arena/MTGO set+collector annotations.
     */
    private ParsedEntry parseQuantityAndName(String line, boolean sideboard, boolean commander) {
        // Strip inline set/collector info: "4 Lightning Bolt (M10) 146" → "4 Lightning Bolt"
        String stripped = stripSetInfo(line);

        // Split on first whitespace group to isolate quantity token
        String[] parts = stripped.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }

        // Strip trailing 'x' from quantity token (e.g. "4x")
        String qtyToken = parts[0].replaceAll("(?i)x$", "");
        int quantity;
        try {
            quantity = Integer.parseInt(qtyToken);
        } catch (NumberFormatException e) {
            return null;
        }

        String cardName = parts[1].strip();
        if (cardName.isBlank()) {
            return null;
        }

        return new ParsedEntry(cardName, quantity, sideboard, commander);
    }

    /**
     * Strip trailing set code / collector number annotations.
     * e.g. "4 Lightning Bolt (M10) 146" → "4 Lightning Bolt"
     */
    private String stripSetInfo(String line) {
        return line.replaceAll("\\s+\\([A-Za-z0-9]+\\)\\s*\\d*\\s*$", "").strip();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private List<String> splitLines(String text) {
        if (text == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(text.split("\\r?\\n"));
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Intermediate representation of a parsed deck-list line before card resolution.
     */
    record ParsedEntry(String cardName, int quantity, boolean isSideboard, boolean isCommander) {}
}
