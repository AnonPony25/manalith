package dev.manalith.catalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.dto.CardPrintingDTO;
import dev.manalith.catalog.dto.CardSearchResultDTO;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.catalog.repository.CardRulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CardCatalogService}.
 *
 * <p>The repository is mocked so no database or Spring context is required.
 */
@ExtendWith(MockitoExtension.class)
class CardCatalogServiceTest {

    @Mock
    private CardPrintingRepository cardPrintingRepository;

    @Mock
    private CardRulingRepository cardRulingRepository;

    private CardCatalogService service;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper to test JSON deserialisation in toDTO()
        service = new CardCatalogService(cardPrintingRepository, cardRulingRepository, new ObjectMapper());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CardPrinting buildCard(UUID id, String name, String setCode) {
        return CardPrinting.builder()
                .id(id)
                .oracleId(UUID.randomUUID())
                .name(name)
                .setCode(setCode)
                .setName("Test Set")
                .collectorNumber("001")
                .rarity("rare")
                .layout("normal")
                .manaCost("{1}{R}")
                .cmc(new BigDecimal("2.0"))
                .typeLine("Instant")
                .oracleText("Deal 3 damage to any target.")
                .colorsJson("[\"R\"]")
                .colorIdentityJson("[\"R\"]")
                .legalitiesJson("{\"standard\":\"legal\"}")
                .imageUrisJson(null)
                .cardFacesJson(null)
                .pricesJson("{\"usd\":\"0.25\",\"usd_foil\":\"1.00\"}")
                .releasedAt(LocalDate.of(2024, 1, 1))
                .updatedAt(Instant.now())
                .build();
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchCards — by name — returns matching cards")
    void searchCards_byName_returnsMatchingCards() {
        UUID id = UUID.randomUUID();
        CardPrinting card = buildCard(id, "Lightning Bolt", "LEA");

        Page<CardPrinting> page = new PageImpl<>(List.of(card));
        when(cardPrintingRepository.findByNameContainingIgnoreCase(eq("Lightning"), any(Pageable.class)))
                .thenReturn(page);

        CardSearchResultDTO result = service.searchCards("Lightning", null, null, null, 0, 20);

        assertThat(result).isNotNull();
        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().get(0).name()).isEqualTo("Lightning Bolt");
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    @DisplayName("searchCards — by name prefix token — parses name: prefix correctly")
    void searchCards_byNameToken_parsesPrefix() {
        UUID id = UUID.randomUUID();
        CardPrinting card = buildCard(id, "Counterspell", "ICE");

        Page<CardPrinting> page = new PageImpl<>(List.of(card));
        when(cardPrintingRepository.findByNameContainingIgnoreCase(eq("Counterspell"), any(Pageable.class)))
                .thenReturn(page);

        CardSearchResultDTO result = service.searchCards("name:Counterspell", null, null, null, 0, 20);

        assertThat(result.cards()).hasSize(1);
        assertThat(result.cards().get(0).name()).isEqualTo("Counterspell");
    }

    @Test
    @DisplayName("getCardById — unknown ID — returns empty Optional")
    void getCardById_unknownId_returnsEmpty() {
        UUID unknownId = UUID.randomUUID();
        when(cardPrintingRepository.findById(unknownId)).thenReturn(Optional.empty());

        Optional<CardPrintingDTO> result = service.getCardById(unknownId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCardById — known ID — returns DTO with correct fields")
    void getCardById_knownId_returnsDto() {
        UUID id = UUID.randomUUID();
        CardPrinting card = buildCard(id, "Dark Ritual", "ICE");
        when(cardPrintingRepository.findById(id)).thenReturn(Optional.of(card));

        Optional<CardPrintingDTO> result = service.getCardById(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id.toString());
        assertThat(result.get().name()).isEqualTo("Dark Ritual");
        assertThat(result.get().setCode()).isEqualTo("ICE");
        assertThat(result.get().cmc()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("getCardByName — fuzzy match — returns card when found")
    void getCardByName_fuzzyMatch_returnsCard() {
        UUID id = UUID.randomUUID();
        CardPrinting card = buildCard(id, "Swords to Plowshares", "LEB");
        when(cardPrintingRepository.findFirstByNameLikeIgnoreCase("%Swords%"))
                .thenReturn(Optional.of(card));

        Optional<CardPrintingDTO> result = service.getCardByName("Swords", true);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Swords to Plowshares");
    }

    @Test
    @DisplayName("getCardByName — exact match — returns empty when not found")
    void getCardByName_exactMatch_returnsEmpty() {
        when(cardPrintingRepository.findFirstByName("Nonexistent Card"))
                .thenReturn(Optional.empty());

        Optional<CardPrintingDTO> result = service.getCardByName("Nonexistent Card", false);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCardByName — blank name — returns empty without querying")
    void getCardByName_blankName_returnsEmpty() {
        Optional<CardPrintingDTO> result = service.getCardByName("", false);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchCards — JSON fields — deserialises colors and prices correctly")
    void searchCards_jsonFields_deserialisedCorrectly() {
        UUID id = UUID.randomUUID();
        CardPrinting card = buildCard(id, "Brainstorm", "ICE");

        Page<CardPrinting> page = new PageImpl<>(List.of(card));
        when(cardPrintingRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(page);

        CardSearchResultDTO result = service.searchCards("Brainstorm", null, null, null, 0, 20);

        CardPrintingDTO dto = result.cards().get(0);
        assertThat(dto.colors()).containsExactly("R");
        assertThat(dto.colorIdentity()).containsExactly("R");
        assertThat(dto.legalities()).containsEntry("standard", "legal");
        assertThat(dto.prices()).isNotNull();
        assertThat(dto.prices().usd()).isEqualTo("0.25");
        assertThat(dto.prices().usdFoil()).isEqualTo("1.00");
    }

    @Test
    @DisplayName("searchCards — format filter — excludes cards not legal in format")
    void searchCards_formatFilter_excludesIllegalCards() {
        UUID id = UUID.randomUUID();
        // Build a card that is NOT legal in standard
        CardPrinting card = buildCard(id, "Black Lotus", "LEA");
        // Override legalities to "banned"
        card.setLegalitiesJson("{\"standard\":\"banned\"}");

        Page<CardPrinting> page = new PageImpl<>(List.of(card));
        when(cardPrintingRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(page);

        // Searching with format=standard should filter out the banned card
        CardSearchResultDTO result = service.searchCards("Black Lotus", null, null, "standard", 0, 20);

        assertThat(result.cards()).isEmpty();
    }
}
