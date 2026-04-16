package dev.manalith.deck;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.deck.dto.LegalityResultDTO;
import dev.manalith.deck.model.Deck;
import dev.manalith.deck.model.DeckEntry;
import dev.manalith.deck.service.DeckLegalityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeckLegalityValidatorTest {

    private DeckLegalityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DeckLegalityValidator(new ObjectMapper());
    }

    // -----------------------------------------------------------------------
    // Helper: build a DeckEntry with a mocked CardPrinting
    // -----------------------------------------------------------------------

    private DeckEntry makeEntry(String oracleId, String typeLine, String legalitiesJson,
                                int quantity, boolean isCommander, boolean isSideboard) {
        CardPrinting card = mock(CardPrinting.class);
        UUID oracleUuid = UUID.nameUUIDFromBytes(oracleId.getBytes());
        UUID cardId = UUID.randomUUID();

        when(card.getId()).thenReturn(cardId);
        when(card.getOracleId()).thenReturn(oracleUuid);
        when(card.getName()).thenReturn("Card-" + oracleId);
        when(card.getTypeLine()).thenReturn(typeLine);
        when(card.getLegalitiesJson()).thenReturn(legalitiesJson);
        when(card.getColorIdentityJson()).thenReturn("[]");
        when(card.getColorsJson()).thenReturn("[]");
        when(card.getCmc()).thenReturn(BigDecimal.TWO);

        DeckEntry entry = DeckEntry.builder()
                .id((long) (Math.random() * 100000))
                .card(card)
                .quantity(quantity)
                .isCommander(isCommander)
                .isSideboard(isSideboard)
                .build();
        return entry;
    }

    /** Build a Deck with the given entries attached. */
    private Deck buildDeck(String format, List<DeckEntry> entries) {
        Deck deck = Deck.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Test Deck")
                .format(format)
                .build();
        entries.forEach(e -> e.setDeck(deck));
        deck.setEntries(entries);
        return deck;
    }

    // -----------------------------------------------------------------------
    // Commander tests
    // -----------------------------------------------------------------------

    @Test
    void commander_exactly100Cards_isLegal() {
        List<DeckEntry> entries = new ArrayList<>();
        // 1 commander
        entries.add(makeEntry("commander-oracle", "Legendary Creature", "{\"commander\":\"legal\"}", 1, true, false));
        // 35 unique non-basic lands
        for (int i = 0; i < 35; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"commander\":\"legal\"}", 1, false, false));
        }
        // 24 basic lands
        for (int i = 0; i < 24; i++) {
            entries.add(makeEntry("basic-" + i, "Basic Land — Forest", "{\"commander\":\"legal\"}", 1, false, false));
        }
        // 40 more spells
        for (int i = 100; i < 140; i++) {
            entries.add(makeEntry("oracle-" + i, "Instant", "{\"commander\":\"legal\"}", 1, false, false));
        }

        // total = 1 + 35 + 24 + 40 = 100
        assertThat(entries.stream().mapToInt(DeckEntry::getQuantity).sum()).isEqualTo(100);

        Deck deck = buildDeck("commander", entries);
        LegalityResultDTO result = validator.validate(deck, "commander");

        assertThat(result.isLegal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void commander_99Cards_isIllegal() {
        List<DeckEntry> entries = new ArrayList<>();
        entries.add(makeEntry("commander-oracle", "Legendary Creature", "{\"commander\":\"legal\"}", 1, true, false));
        for (int i = 0; i < 98; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"commander\":\"legal\"}", 1, false, false));
        }
        // total = 99

        Deck deck = buildDeck("commander", entries);
        LegalityResultDTO result = validator.validate(deck, "commander");

        assertThat(result.isLegal()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("100 cards"));
    }

    @Test
    void commander_duplicateNonBasic_isIllegal() {
        List<DeckEntry> entries = new ArrayList<>();
        entries.add(makeEntry("commander-oracle", "Legendary Creature", "{\"commander\":\"legal\"}", 1, true, false));
        // Same oracle id twice (non-basic)
        entries.add(makeEntry("dupe-oracle", "Creature", "{\"commander\":\"legal\"}", 2, false, false));
        // Fill to 100
        for (int i = 0; i < 97; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"commander\":\"legal\"}", 1, false, false));
        }
        // total = 1 + 2 + 97 = 100

        Deck deck = buildDeck("commander", entries);
        LegalityResultDTO result = validator.validate(deck, "commander");

        assertThat(result.isLegal()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("Duplicate"));
    }

    @Test
    void commander_noCommanderEntry_isIllegal() {
        List<DeckEntry> entries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"commander\":\"legal\"}", 1, false, false));
        }

        Deck deck = buildDeck("commander", entries);
        LegalityResultDTO result = validator.validate(deck, "commander");

        assertThat(result.isLegal()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("commander"));
    }

    // -----------------------------------------------------------------------
    // Standard tests
    // -----------------------------------------------------------------------

    @Test
    void standard_60Cards_4copiesMax_isLegal() {
        List<DeckEntry> entries = new ArrayList<>();
        // 15 groups of 4 cards each = 60 cards
        for (int i = 0; i < 15; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"standard\":\"legal\"}", 4, false, false));
        }

        Deck deck = buildDeck("standard", entries);
        LegalityResultDTO result = validator.validate(deck, "standard");

        assertThat(result.isLegal()).isTrue();
    }

    @Test
    void standard_fiveCopiesOfCard_isIllegal() {
        List<DeckEntry> entries = new ArrayList<>();
        // 5 copies of same card
        entries.add(makeEntry("dupe-oracle", "Creature", "{\"standard\":\"legal\"}", 5, false, false));
        // Fill up to 60
        for (int i = 0; i < 55; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"standard\":\"legal\"}", 1, false, false));
        }

        Deck deck = buildDeck("standard", entries);
        LegalityResultDTO result = validator.validate(deck, "standard");

        assertThat(result.isLegal()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("5 times"));
    }

    @Test
    void standard_bannedCard_isIllegal() {
        List<DeckEntry> entries = new ArrayList<>();
        // 1 banned card
        entries.add(makeEntry("banned-oracle", "Creature", "{\"standard\":\"banned\"}", 1, false, false));
        // 59 legal cards
        for (int i = 0; i < 59; i++) {
            entries.add(makeEntry("oracle-" + i, "Creature", "{\"standard\":\"legal\"}", 1, false, false));
        }

        Deck deck = buildDeck("standard", entries);
        LegalityResultDTO result = validator.validate(deck, "standard");

        assertThat(result.isLegal()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("banned"));
    }

    // -----------------------------------------------------------------------
    // Custom test
    // -----------------------------------------------------------------------

    @Test
    void custom_anyDeck_isLegal() {
        List<DeckEntry> entries = new ArrayList<>();
        // Even a completely empty deck is legal in custom
        Deck deck = buildDeck("custom", entries);
        LegalityResultDTO result = validator.validate(deck, "custom");

        assertThat(result.isLegal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }
}
