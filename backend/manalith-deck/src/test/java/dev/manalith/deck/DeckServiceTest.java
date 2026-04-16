package dev.manalith.deck;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.deck.dto.*;
import dev.manalith.deck.exception.*;
import dev.manalith.deck.model.Deck;
import dev.manalith.deck.model.DeckEntry;
import dev.manalith.deck.repository.DeckEntryRepository;
import dev.manalith.deck.repository.DeckRepository;
import dev.manalith.deck.service.DeckLegalityValidator;
import dev.manalith.deck.service.DeckService;
import dev.manalith.deck.service.DeckStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckEntryRepository deckEntryRepository;

    @Mock
    private CardPrintingRepository cardPrintingRepository;

    @Mock
    private DeckLegalityValidator deckLegalityValidator;

    @Mock
    private DeckStatsService deckStatsService;

    private DeckService deckService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        deckService = new DeckService(
                deckRepository,
                deckEntryRepository,
                cardPrintingRepository,
                deckLegalityValidator,
                deckStatsService,
                objectMapper
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Deck buildDeck(UUID deckId, UUID ownerId, String format, boolean isPublic) {
        Deck deck = Deck.builder()
                .id(deckId)
                .ownerId(ownerId)
                .name("Test Deck")
                .format(format)
                .isPublic(isPublic)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        deck.setEntries(new ArrayList<>());
        return deck;
    }

    private DeckStatsDTO emptyStats() {
        return new DeckStatsDTO(0, 0, 0,
                Map.of("W", 0, "U", 0, "B", 0, "R", 0, "G", 0, "C", 0),
                Map.of("0", 0, "1", 0, "2", 0, "3", 0, "4", 0, "5", 0, "6", 0, "7+", 0),
                Map.of("Creature", 0, "Land", 0),
                0.0, List.of(), null);
    }

    private LegalityResultDTO legalResult(String format) {
        return LegalityResultDTO.legal(format);
    }

    // -----------------------------------------------------------------------
    // createDeck
    // -----------------------------------------------------------------------

    @Test
    void createDeck_underLimit_succeeds() {
        UUID ownerId = UUID.randomUUID();
        CreateDeckRequest req = new CreateDeckRequest("My Deck", "standard", "desc", false);

        when(deckRepository.countByOwnerId(ownerId)).thenReturn(10L);
        Deck saved = buildDeck(UUID.randomUUID(), ownerId, "standard", false);
        when(deckRepository.save(any(Deck.class))).thenReturn(saved);

        DeckSummaryDTO result = deckService.createDeck(ownerId, req);

        assertThat(result).isNotNull();
        assertThat(result.ownerId()).isEqualTo(ownerId.toString());
        verify(deckRepository).save(any(Deck.class));
    }

    @Test
    void createDeck_overLimit_throwsDeckLimitExceededException() {
        UUID ownerId = UUID.randomUUID();
        CreateDeckRequest req = new CreateDeckRequest("My Deck", "standard", null, false);

        when(deckRepository.countByOwnerId(ownerId)).thenReturn(200L);

        assertThatThrownBy(() -> deckService.createDeck(ownerId, req))
                .isInstanceOf(DeckLimitExceededException.class);

        verify(deckRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // getDeckDetail
    // -----------------------------------------------------------------------

    @Test
    void getDeckDetail_publicDeck_accessibleByNonOwner() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();

        Deck deck = buildDeck(deckId, ownerId, "standard", true);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(deckStatsService.computeStats(any(), any(), any())).thenReturn(emptyStats());
        when(deckLegalityValidator.validate(any(), any())).thenReturn(legalResult("standard"));

        DeckDetailDTO result = deckService.getDeckDetail(deckId, nonOwnerId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(deckId.toString());
    }

    @Test
    void getDeckDetail_privateDeck_forbiddenForNonOwner() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();

        Deck deck = buildDeck(deckId, ownerId, "standard", false);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        assertThatThrownBy(() -> deckService.getDeckDetail(deckId, nonOwnerId))
                .isInstanceOf(DeckAccessDeniedException.class);
    }

    // -----------------------------------------------------------------------
    // addCard
    // -----------------------------------------------------------------------

    @Test
    void addCard_newEntry_savesEntry() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        AddCardRequest req = new AddCardRequest(cardId.toString(), 2, false, false);

        when(deckRepository.existsByIdAndOwnerId(deckId, ownerId)).thenReturn(true);

        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(cardId);
        when(card.getOracleId()).thenReturn(UUID.randomUUID());
        when(card.getName()).thenReturn("Lightning Bolt");
        when(card.getTypeLine()).thenReturn("Instant");
        when(card.getManaCost()).thenReturn("{R}");
        when(card.getCmc()).thenReturn(BigDecimal.ONE);
        when(card.getSetCode()).thenReturn("M10");
        when(card.getRarity()).thenReturn("common");
        when(card.getImageUrisJson()).thenReturn("{\"normal\":\"https://example.com/img.jpg\"}");
        when(card.getCardFacesJson()).thenReturn(null);
        when(cardPrintingRepository.findById(cardId)).thenReturn(Optional.of(card));

        Deck deck = buildDeck(deckId, ownerId, "standard", false);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(deckEntryRepository.findByDeckIdAndCardIdAndIsSideboard(deckId, cardId, false))
                .thenReturn(Optional.empty());
        when(deckStatsService.computeStats(any(), any(), any())).thenReturn(emptyStats());
        when(deckLegalityValidator.validate(any(), any())).thenReturn(legalResult("standard"));

        DeckDetailDTO result = deckService.addCard(deckId, ownerId, req);

        assertThat(result).isNotNull();
        verify(deckEntryRepository).save(any(DeckEntry.class));
    }

    @Test
    void addCard_existingEntry_updatesQuantity() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        AddCardRequest req = new AddCardRequest(cardId.toString(), 2, false, false);

        when(deckRepository.existsByIdAndOwnerId(deckId, ownerId)).thenReturn(true);

        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(cardId);
        when(card.getOracleId()).thenReturn(UUID.randomUUID());
        when(card.getName()).thenReturn("Counterspell");
        when(card.getTypeLine()).thenReturn("Instant");
        when(card.getManaCost()).thenReturn("{U}{U}");
        when(card.getCmc()).thenReturn(BigDecimal.TWO);
        when(card.getSetCode()).thenReturn("LEA");
        when(card.getRarity()).thenReturn("common");
        when(card.getImageUrisJson()).thenReturn("{\"normal\":\"https://example.com/img.jpg\"}");
        when(card.getCardFacesJson()).thenReturn(null);
        when(cardPrintingRepository.findById(cardId)).thenReturn(Optional.of(card));

        Deck deck = buildDeck(deckId, ownerId, "standard", false);

        DeckEntry existing = DeckEntry.builder()
                .id(1L)
                .deck(deck)
                .card(card)
                .quantity(2)
                .isCommander(false)
                .isSideboard(false)
                .build();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(deckEntryRepository.findByDeckIdAndCardIdAndIsSideboard(deckId, cardId, false))
                .thenReturn(Optional.of(existing));
        when(deckStatsService.computeStats(any(), any(), any())).thenReturn(emptyStats());
        when(deckLegalityValidator.validate(any(), any())).thenReturn(legalResult("standard"));

        deckService.addCard(deckId, ownerId, req);

        // Quantity should be updated to 4 (2 existing + 2 added)
        assertThat(existing.getQuantity()).isEqualTo(4);
        verify(deckEntryRepository).save(existing);
    }

    // -----------------------------------------------------------------------
    // removeCard
    // -----------------------------------------------------------------------

    @Test
    void removeCard_existingEntry_deletesEntry() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        when(deckRepository.existsByIdAndOwnerId(deckId, ownerId)).thenReturn(true);

        CardPrinting card = mock(CardPrinting.class);
        when(card.getId()).thenReturn(cardId);

        Deck deck = buildDeck(deckId, ownerId, "standard", false);
        DeckEntry entry = DeckEntry.builder()
                .id(1L)
                .deck(deck)
                .card(card)
                .quantity(1)
                .build();

        when(deckEntryRepository.findByDeckIdAndCardIdAndIsSideboard(deckId, cardId, false))
                .thenReturn(Optional.of(entry));

        deckService.removeCard(deckId, ownerId, cardId.toString(), false);

        verify(deckEntryRepository).delete(entry);
    }

    // -----------------------------------------------------------------------
    // updateDeck
    // -----------------------------------------------------------------------

    @Test
    void updateDeck_nonOwner_throwsNotFoundException() {
        UUID deckId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();
        UpdateDeckRequest req = new UpdateDeckRequest("New Name", null, null, null);

        when(deckRepository.existsByIdAndOwnerId(deckId, nonOwnerId)).thenReturn(false);

        assertThatThrownBy(() -> deckService.updateDeck(deckId, nonOwnerId, req))
                .isInstanceOf(DeckNotFoundException.class);

        verify(deckRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // validateDeck
    // -----------------------------------------------------------------------

    @Test
    void validateDeck_delegatesToValidator() {
        UUID deckId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Deck deck = buildDeck(deckId, requesterId, "standard", true);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(deckLegalityValidator.validate(deck, "standard"))
                .thenReturn(LegalityResultDTO.legal("standard"));

        LegalityResultDTO result = deckService.validateDeck(deckId, requesterId);

        assertThat(result.isLegal()).isTrue();
        verify(deckLegalityValidator).validate(deck, "standard");
    }
}
