package dev.manalith.deck;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.deck.dto.DeckStatsDTO;
import dev.manalith.deck.model.Deck;
import dev.manalith.deck.model.DeckEntry;
import dev.manalith.deck.service.DeckStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeckStatsServiceTest {

    private DeckStatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new DeckStatsService(new ObjectMapper());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private DeckEntry makeEntry(String typeLine, String colorsJson, String colorIdentityJson,
                                double cmc, int quantity) {
        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(UUID.randomUUID());
        when(card.getOracleId()).thenReturn(UUID.randomUUID());
        when(card.getName()).thenReturn("TestCard-" + typeLine);
        when(card.getTypeLine()).thenReturn(typeLine);
        when(card.getColorsJson()).thenReturn(colorsJson);
        when(card.getColorIdentityJson()).thenReturn(colorIdentityJson);
        when(card.getCmc()).thenReturn(BigDecimal.valueOf(cmc));

        return DeckEntry.builder()
                .id((long) (Math.random() * 100000))
                .card(card)
                .quantity(quantity)
                .isCommander(false)
                .isSideboard(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // Mana curve tests
    // -----------------------------------------------------------------------

    @Test
    void manaCurve_groupsByFlooredCmc() {
        DeckEntry entry2 = makeEntry("Creature", "[\"R\"]", "[\"R\"]", 2.0, 4);
        DeckEntry entry3 = makeEntry("Instant", "[\"U\"]", "[\"U\"]", 3.0, 3);

        DeckStatsDTO stats = statsService.computeStats(List.of(entry2, entry3), List.of(), "standard");

        assertThat(stats.manaCurve().get("2")).isEqualTo(4);
        assertThat(stats.manaCurve().get("3")).isEqualTo(3);
    }

    @Test
    void manaCurve_7PlusBucket() {
        DeckEntry entry = makeEntry("Creature", "[\"G\"]", "[\"G\"]", 8.0, 2);

        DeckStatsDTO stats = statsService.computeStats(List.of(entry), List.of(), "standard");

        assertThat(stats.manaCurve().get("7+")).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Color distribution tests
    // -----------------------------------------------------------------------

    @Test
    void colorDistribution_multiColorCard_countsAllColors() {
        // A Gruul (R+G) creature with quantity 3
        DeckEntry entry = makeEntry("Creature", "[\"R\",\"G\"]", "[\"R\",\"G\"]", 3.0, 3);

        DeckStatsDTO stats = statsService.computeStats(List.of(entry), List.of(), "standard");

        assertThat(stats.colorDistribution().get("R")).isEqualTo(3);
        assertThat(stats.colorDistribution().get("G")).isEqualTo(3);
        assertThat(stats.colorDistribution().get("W")).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Type breakdown tests
    // -----------------------------------------------------------------------

    @Test
    void typeBreakdown_creatureCountsCorrectly() {
        DeckEntry creature = makeEntry("Creature — Elf Druid", "[\"G\"]", "[\"G\"]", 2.0, 4);
        DeckEntry instant = makeEntry("Instant", "[\"U\"]", "[\"U\"]", 1.0, 3);

        DeckStatsDTO stats = statsService.computeStats(List.of(creature, instant), List.of(), "standard");

        assertThat(stats.typeBreakdown().get("Creature")).isEqualTo(4);
        assertThat(stats.typeBreakdown().get("Instant")).isEqualTo(3);
    }

    @Test
    void typeBreakdown_landExcludedFromCmc() {
        DeckEntry land = makeEntry("Basic Land — Forest", "[]", "[\"G\"]", 0.0, 24);
        DeckEntry creature = makeEntry("Creature", "[\"G\"]", "[\"G\"]", 2.0, 4);

        DeckStatsDTO stats = statsService.computeStats(List.of(land, creature), List.of(), "standard");

        // Land should appear in type breakdown
        assertThat(stats.typeBreakdown().get("Land")).isEqualTo(24);
        // Land NOT in mana curve
        assertThat(stats.manaCurve().get("0")).isEqualTo(0);
        assertThat(stats.manaCurve().get("2")).isEqualTo(4);
    }

    // -----------------------------------------------------------------------
    // Average CMC tests
    // -----------------------------------------------------------------------

    @Test
    void averageCmc_excludesLands() {
        DeckEntry land = makeEntry("Basic Land — Mountain", "[]", "[\"R\"]", 0.0, 20);
        // 4 creatures @ cmc 2 and 4 creatures @ cmc 4 → avg = (8+16)/8 = 3.0
        DeckEntry creature1 = makeEntry("Creature", "[\"R\"]", "[\"R\"]", 2.0, 4);
        DeckEntry creature2 = makeEntry("Creature", "[\"R\"]", "[\"R\"]", 4.0, 4);

        DeckStatsDTO stats = statsService.computeStats(List.of(land, creature1, creature2), List.of(), "standard");

        assertThat(stats.averageCmc()).isCloseTo(3.0, within(0.01));
    }
}
