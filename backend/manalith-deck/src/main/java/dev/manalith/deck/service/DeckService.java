package dev.manalith.deck.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.deck.dto.*;
import dev.manalith.deck.exception.*;
import dev.manalith.deck.model.Deck;
import dev.manalith.deck.model.DeckEntry;
import dev.manalith.deck.repository.DeckEntryRepository;
import dev.manalith.deck.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeckService {

    public static final int MAX_DECKS_PER_USER = 200;

    private final DeckRepository deckRepository;
    private final DeckEntryRepository deckEntryRepository;
    private final CardPrintingRepository cardPrintingRepository;
    private final DeckLegalityValidator deckLegalityValidator;
    private final DeckStatsService deckStatsService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // Create
    // =========================================================================

    public DeckSummaryDTO createDeck(UUID ownerId, CreateDeckRequest req) {
        long existingCount = deckRepository.countByOwnerId(ownerId);
        if (existingCount >= MAX_DECKS_PER_USER) {
            throw new DeckLimitExceededException(MAX_DECKS_PER_USER);
        }

        Deck deck = Deck.builder()
                .ownerId(ownerId)
                .name(req.name())
                .format(req.format())
                .description(req.description())
                .isPublic(req.isPublic())
                .build();

        deck = deckRepository.save(deck);
        return toDeckSummaryDTO(deck);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DeckSummaryDTO> getMyDecks(UUID ownerId) {
        return deckRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream()
                .map(this::toDeckSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DeckSummaryDTO> getPublicDecks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return deckRepository.findByIsPublicTrueOrderByUpdatedAtDesc(pageable)
                .map(this::toDeckSummaryDTO);
    }

    @Transactional(readOnly = true)
    public DeckDetailDTO getDeckDetail(UUID deckId, UUID requesterId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.isPublic() && !deck.getOwnerId().equals(requesterId)) {
            throw new DeckAccessDeniedException();
        }

        return buildDeckDetailDTO(deck);
    }

    // =========================================================================
    // Update
    // =========================================================================

    public DeckSummaryDTO updateDeck(UUID deckId, UUID ownerId, UpdateDeckRequest req) {
        if (!deckRepository.existsByIdAndOwnerId(deckId, ownerId)) {
            throw new DeckNotFoundException(deckId);
        }

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (req.name() != null) {
            deck.setName(req.name());
        }
        if (req.format() != null) {
            deck.setFormat(req.format());
        }
        if (req.description() != null) {
            deck.setDescription(req.description());
        }
        if (req.isPublic() != null) {
            deck.setPublic(req.isPublic());
        }

        deck = deckRepository.save(deck);
        return toDeckSummaryDTO(deck);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void deleteDeck(UUID deckId, UUID ownerId) {
        if (!deckRepository.existsByIdAndOwnerId(deckId, ownerId)) {
            throw new DeckNotFoundException(deckId);
        }
        deckRepository.deleteById(deckId);
    }

    // =========================================================================
    // Card management
    // =========================================================================

    public DeckDetailDTO addCard(UUID deckId, UUID ownerId, AddCardRequest req) {
        verifyOwnership(deckId, ownerId);

        UUID cardId = UUID.fromString(req.cardId());
        CardPrinting card = cardPrintingRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        // Commander slot handling
        if (req.isCommander()) {
            if (!"commander".equalsIgnoreCase(deck.getFormat())) {
                throw new IllegalArgumentException("Only Commander format decks can have a commander designation.");
            }
            // Clear existing commander flag
            deck.getEntries().stream()
                    .filter(DeckEntry::isCommander)
                    .forEach(e -> e.setCommander(false));
            deckEntryRepository.saveAll(deck.getEntries().stream()
                    .filter(e -> !e.isCommander()).collect(Collectors.toList()));
        }

        // Find existing entry (same card + same sideboard zone)
        Optional<DeckEntry> existing = deckEntryRepository
                .findByDeckIdAndCardIdAndIsSideboard(deckId, cardId, req.isSideboard());

        if (existing.isPresent()) {
            DeckEntry entry = existing.get();
            entry.setQuantity(entry.getQuantity() + req.quantity());
            if (req.isCommander()) {
                entry.setCommander(true);
            }
            deckEntryRepository.save(entry);
        } else {
            DeckEntry newEntry = DeckEntry.builder()
                    .deck(deck)
                    .card(card)
                    .quantity(req.quantity())
                    .isCommander(req.isCommander())
                    .isSideboard(req.isSideboard())
                    .build();
            deckEntryRepository.save(newEntry);
        }

        // Re-fetch fresh deck
        Deck refreshed = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));
        return buildDeckDetailDTO(refreshed);
    }

    public void removeCard(UUID deckId, UUID ownerId, String cardId, boolean isSideboard) {
        verifyOwnership(deckId, ownerId);

        UUID cardUuid = UUID.fromString(cardId);
        DeckEntry entry = deckEntryRepository
                .findByDeckIdAndCardIdAndIsSideboard(deckId, cardUuid, isSideboard)
                .orElseThrow(() -> new CardNotFoundException("Card not found in deck: " + cardId));

        deckEntryRepository.delete(entry);
    }

    public void updateCardQuantity(UUID deckId, UUID ownerId, String cardId, boolean isSideboard, int quantity) {
        verifyOwnership(deckId, ownerId);

        UUID cardUuid = UUID.fromString(cardId);
        DeckEntry entry = deckEntryRepository
                .findByDeckIdAndCardIdAndIsSideboard(deckId, cardUuid, isSideboard)
                .orElseThrow(() -> new CardNotFoundException("Card not found in deck: " + cardId));

        if (quantity <= 0) {
            deckEntryRepository.delete(entry);
        } else {
            entry.setQuantity(quantity);
            deckEntryRepository.save(entry);
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Transactional(readOnly = true)
    public LegalityResultDTO validateDeck(UUID deckId, UUID requesterId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.isPublic() && !deck.getOwnerId().equals(requesterId)) {
            throw new DeckAccessDeniedException();
        }

        return deckLegalityValidator.validate(deck, deck.getFormat());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void verifyOwnership(UUID deckId, UUID ownerId) {
        if (!deckRepository.existsByIdAndOwnerId(deckId, ownerId)) {
            throw new DeckNotFoundException(deckId);
        }
    }

    private DeckDetailDTO buildDeckDetailDTO(Deck deck) {
        List<DeckEntry> allEntries = deck.getEntries();

        List<DeckEntry> mainboard = allEntries.stream()
                .filter(e -> !e.isSideboard())
                .collect(Collectors.toList());
        List<DeckEntry> sideboard = allEntries.stream()
                .filter(DeckEntry::isSideboard)
                .collect(Collectors.toList());
        List<DeckEntry> commanders = allEntries.stream()
                .filter(DeckEntry::isCommander)
                .collect(Collectors.toList());

        List<DeckEntryDTO> mainboardDTOs = mainboard.stream()
                .filter(e -> !e.isCommander())
                .map(this::toDeckEntryDTO)
                .collect(Collectors.toList());
        List<DeckEntryDTO> sideboardDTOs = sideboard.stream()
                .map(this::toDeckEntryDTO)
                .collect(Collectors.toList());
        List<DeckEntryDTO> commanderDTOs = commanders.stream()
                .map(this::toDeckEntryDTO)
                .collect(Collectors.toList());

        // Stats (without legality)
        DeckStatsDTO statsNoLegality = deckStatsService.computeStats(mainboard, sideboard, deck.getFormat());

        // Legality
        LegalityResultDTO legality = deckLegalityValidator.validate(deck, deck.getFormat());

        // Assemble final stats with legality
        DeckStatsDTO stats = new DeckStatsDTO(
                statsNoLegality.totalCards(),
                statsNoLegality.uniqueCards(),
                statsNoLegality.sideboardCount(),
                statsNoLegality.colorDistribution(),
                statsNoLegality.manaCurve(),
                statsNoLegality.typeBreakdown(),
                statsNoLegality.averageCmc(),
                statsNoLegality.colorIdentity(),
                legality
        );

        return new DeckDetailDTO(
                deck.getId().toString(),
                deck.getOwnerId().toString(),
                null, // ownerName resolved elsewhere (e.g., controller/user service)
                deck.getName(),
                deck.getFormat(),
                deck.getDescription(),
                deck.isPublic(),
                deck.getCreatedAt() != null ? deck.getCreatedAt().toString() : null,
                deck.getUpdatedAt() != null ? deck.getUpdatedAt().toString() : null,
                mainboardDTOs,
                sideboardDTOs,
                commanderDTOs,
                stats
        );
    }

    private DeckEntryDTO toDeckEntryDTO(DeckEntry entry) {
        CardPrinting card = entry.getCard();
        String imageUri = extractNormalImageUri(card.getImageUrisJson(), card.getCardFacesJson());
        double cmc = card.getCmc() != null ? card.getCmc().doubleValue() : 0.0;

        return new DeckEntryDTO(
                card.getId().toString(),
                card.getName(),
                card.getManaCost(),
                cmc,
                card.getTypeLine(),
                card.getSetCode(),
                card.getRarity(),
                imageUri,
                entry.getQuantity(),
                entry.isCommander()
        );
    }

    private String extractNormalImageUri(String imageUrisJson, String cardFacesJson) {
        // Try top-level imageUris first
        if (imageUrisJson != null && !imageUrisJson.isBlank()) {
            try {
                Map<String, String> imageUris = objectMapper.readValue(imageUrisJson,
                        new TypeReference<Map<String, String>>() {});
                String normal = imageUris.get("normal");
                if (normal != null) return normal;
            } catch (Exception e) {
                log.warn("Failed to parse imageUrisJson: {}", imageUrisJson, e);
            }
        }

        // Fallback to first face's imageUris.normal
        if (cardFacesJson != null && !cardFacesJson.isBlank()) {
            try {
                List<Map<String, Object>> faces = objectMapper.readValue(cardFacesJson,
                        new TypeReference<List<Map<String, Object>>>() {});
                if (!faces.isEmpty()) {
                    Object faceImageUrisObj = faces.get(0).get("image_uris");
                    if (faceImageUrisObj instanceof Map<?, ?> faceImageUris) {
                        Object normal = faceImageUris.get("normal");
                        if (normal instanceof String s) return s;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse cardFacesJson: {}", cardFacesJson, e);
            }
        }

        return null;
    }

    private DeckSummaryDTO toDeckSummaryDTO(Deck deck) {
        List<DeckEntry> entries = deck.getEntries();
        int mainboardCount = entries.stream()
                .filter(e -> !e.isSideboard())
                .mapToInt(DeckEntry::getQuantity)
                .sum();
        int sideboardCount = entries.stream()
                .filter(DeckEntry::isSideboard)
                .mapToInt(DeckEntry::getQuantity)
                .sum();

        return new DeckSummaryDTO(
                deck.getId().toString(),
                deck.getOwnerId().toString(),
                null, // ownerName resolved by caller if needed
                deck.getName(),
                deck.getFormat(),
                deck.getDescription(),
                deck.isPublic(),
                mainboardCount,
                sideboardCount,
                deck.getCreatedAt() != null ? deck.getCreatedAt().toString() : null,
                deck.getUpdatedAt() != null ? deck.getUpdatedAt().toString() : null
        );
    }
}
