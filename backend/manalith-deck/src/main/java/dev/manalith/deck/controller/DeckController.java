package dev.manalith.deck.controller;

import dev.manalith.deck.dto.*;
import dev.manalith.deck.service.DeckExportService;
import dev.manalith.deck.service.DeckImportService;
import dev.manalith.deck.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for deck management endpoints.
 *
 * <p>Mutation endpoints require authentication (enforced by SecurityConfig).
 * Public-read endpoints (browse public decks, view public deck details) are
 * accessible without a token.
 */
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;
    private final DeckImportService deckImportService;
    private final DeckExportService deckExportService;

    // -------------------------------------------------------------------------
    // Inline record for card-quantity update body (private static per spec)
    // -------------------------------------------------------------------------

    private static record UpdateCardQuantityRequest(int quantity, boolean sideboard) {}

    // -------------------------------------------------------------------------
    // Import request record
    // -------------------------------------------------------------------------

    public record ImportDeckRequest(String name, String format, String deckList, String importFormat) {}

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    /**
     * List the authenticated user's own decks.
     */
    @GetMapping
    public List<DeckSummaryDTO> listMyDecks() {
        UUID ownerId = currentUserId();
        return deckService.getMyDecks(ownerId);
    }

    /**
     * Browse public decks (no auth required).
     */
    @GetMapping("/public")
    public ResponseEntity<Page<DeckSummaryDTO>> listPublicDecks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DeckSummaryDTO> result = deckService.getPublicDecks(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Get full deck detail. Public decks are accessible to anyone; private decks
     * require the requester to be the owner. Anonymous requests use a random UUID
     * so private decks are always denied.
     */
    @GetMapping("/{id}")
    public DeckDetailDTO getDeck(@PathVariable UUID id) {
        UUID requesterId = currentUserIdOrAnonymous();
        return deckService.getDeckDetail(id, requesterId);
    }

    // -------------------------------------------------------------------------
    // Mutation endpoints (all require auth)
    // -------------------------------------------------------------------------

    /**
     * Create a new deck. Returns 201 Created with a Location header.
     */
    @PostMapping
    public ResponseEntity<DeckSummaryDTO> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        UUID ownerId = currentUserId();
        DeckSummaryDTO created = deckService.createDeck(ownerId, request);
        URI location = URI.create("/api/decks/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Update deck metadata (name, format, visibility, description).
     * All fields in the request body are optional (nullable).
     */
    @PatchMapping("/{id}")
    public DeckSummaryDTO updateDeck(
            @PathVariable UUID id,
            @RequestBody UpdateDeckRequest request) {
        UUID requesterId = currentUserId();
        return deckService.updateDeck(id, requesterId, request);
    }

    /**
     * Delete a deck. Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable UUID id) {
        UUID requesterId = currentUserId();
        deckService.deleteDeck(id, requesterId);
    }

    /**
     * Add a card to a deck.
     */
    @PostMapping("/{id}/cards")
    public DeckDetailDTO addCard(
            @PathVariable UUID id,
            @Valid @RequestBody AddCardRequest request) {
        UUID requesterId = currentUserId();
        return deckService.addCard(id, requesterId, request);
    }

    /**
     * Remove a card from a deck (mainboard or sideboard).
     */
    @DeleteMapping("/{id}/cards/{cardId}")
    public DeckDetailDTO removeCard(
            @PathVariable UUID id,
            @PathVariable UUID cardId,
            @RequestParam(defaultValue = "false") boolean sideboard) {
        UUID requesterId = currentUserId();
        deckService.removeCard(id, requesterId, cardId.toString(), sideboard);
        return deckService.getDeckDetail(id, requesterId);
    }

    /**
     * Update the quantity of a card already in a deck.
     */
    @PatchMapping("/{id}/cards/{cardId}")
    public DeckDetailDTO updateCardQuantity(
            @PathVariable UUID id,
            @PathVariable UUID cardId,
            @RequestBody UpdateCardQuantityRequest request) {
        UUID requesterId = currentUserId();
        deckService.updateCardQuantity(id, requesterId, cardId.toString(), request.sideboard(), request.quantity());
        return deckService.getDeckDetail(id, requesterId);
    }

    // -------------------------------------------------------------------------
    // Validation & legality
    // -------------------------------------------------------------------------

    /**
     * Check format legality for the deck.
     */
    @GetMapping("/{id}/validate")
    public LegalityResultDTO validateDeck(@PathVariable UUID id) {
        UUID requesterId = currentUserIdOrAnonymous();
        return deckService.validateDeck(id, requesterId);
    }

    // -------------------------------------------------------------------------
    // Export & Import
    // -------------------------------------------------------------------------

    /**
     * Export a deck to a text representation.
     * Supported formats: text, mtgo, arena, json.
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<String> exportDeck(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "text") String format) {
        UUID requesterId = currentUserIdOrAnonymous();
        String content = deckExportService.export(id, requesterId, format);

        MediaType contentType = "json".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_JSON
                : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(content);
    }

    /**
     * Import a deck from a text representation.
     * Returns 201 Created with the newly imported deck detail.
     */
    @PostMapping("/import")
    public ResponseEntity<DeckDetailDTO> importDeck(@RequestBody ImportDeckRequest request) {
        UUID ownerId = currentUserId();
        DeckDetailDTO imported = deckImportService.importDeck(
                ownerId,
                request.name(),
                request.format(),
                request.deckList(),
                request.importFormat()
        );
        URI location = URI.create("/api/decks/" + imported.id());
        return ResponseEntity.created(location).body(imported);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the UUID of the currently authenticated user.
     * Throws if there is no authentication (should be blocked upstream by SecurityConfig).
     */
    private UUID currentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Returns the UUID of the currently authenticated user, or a random UUID for
     * anonymous requests. Using a random UUID ensures private decks are always
     * inaccessible to unauthenticated callers without leaking ownership info.
     */
    private UUID currentUserIdOrAnonymous() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID uuid) {
            return uuid;
        }
        return UUID.randomUUID();
    }
}
