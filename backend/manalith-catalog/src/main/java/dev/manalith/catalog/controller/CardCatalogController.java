package dev.manalith.catalog.controller;

import dev.manalith.catalog.dto.CardPrintingDTO;
import dev.manalith.catalog.dto.CardSearchResultDTO;
import dev.manalith.catalog.service.CardCatalogService;
import dev.manalith.catalog.sync.ScryfallBulkSyncJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the card catalog API.
 *
 * <p>Base path: {@code /api/catalog}
 *
 * <h2>Endpoints</h2>
 * <pre>
 *   GET  /api/catalog/cards/search          Search cards with optional filters
 *   GET  /api/catalog/cards/{id}            Get a single card printing by UUID
 *   GET  /api/catalog/cards/named           Look up a card by exact or fuzzy name
 *   GET  /api/catalog/cards/{id}/rulings    Get rulings for a card's Oracle ID
 *   POST /api/catalog/sync                  Trigger a manual Scryfall bulk sync
 *   GET  /api/catalog/sets                  List known sets (stub)
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CardCatalogController {

    private final CardCatalogService cardCatalogService;
    private final ScryfallBulkSyncJob scryfallBulkSyncJob;

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Full-text card search with optional filtering.
     *
     * @param q      Scryfall-like query string (e.g. {@code name:Lightning set:LCI})
     * @param set    explicit set code filter (e.g. {@code MOM})
     * @param color  explicit colour filter (e.g. {@code WU})
     * @param format legality format filter (e.g. {@code standard})
     * @param page   zero-based page index (default 0)
     * @param size   results per page (default 20, max 200)
     */
    @GetMapping("/cards/search")
    public ResponseEntity<CardSearchResultDTO> search(
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "set", required = false, defaultValue = "") String set,
            @RequestParam(value = "color", required = false, defaultValue = "") String color,
            @RequestParam(value = "format", required = false, defaultValue = "") String format,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        CardSearchResultDTO result = cardCatalogService.searchCards(q, set, color, format, page, size);
        return ResponseEntity.ok(result);
    }

    // ─── Named lookup — must be declared BEFORE /{id} to avoid route clash ───

    /**
     * Look up a card by exact or fuzzy name.
     *
     * <p>Exactly one of {@code exact} or {@code fuzzy} must be provided.
     *
     * @param exact exact card name (case-sensitive)
     * @param fuzzy fuzzy/substring name match
     */
    @GetMapping("/cards/named")
    public ResponseEntity<CardPrintingDTO> getByName(
            @RequestParam(value = "exact", required = false) String exact,
            @RequestParam(value = "fuzzy", required = false) String fuzzy
    ) {
        if (exact != null && !exact.isBlank()) {
            return cardCatalogService.getCardByName(exact, false)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if (fuzzy != null && !fuzzy.isBlank()) {
            return cardCatalogService.getCardByName(fuzzy, true)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().build();
    }

    // ─── Single card by ID ────────────────────────────────────────────────────

    /**
     * Return a single card printing by its Scryfall UUID.
     *
     * @param id Scryfall UUID string
     */
    @GetMapping("/cards/{id}")
    public ResponseEntity<CardPrintingDTO> getById(@PathVariable("id") String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return cardCatalogService.getCardById(uuid)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ─── Rulings ─────────────────────────────────────────────────────────────

    /**
     * Return all rulings for the card with the given Scryfall UUID.
     *
     * <p>Rulings are retrieved by the card's Oracle ID, so the response covers
     * all printings of the same card.
     *
     * @param id Scryfall UUID string of any printing
     */
    @GetMapping("/cards/{id}/rulings")
    public ResponseEntity<List<CardCatalogService.CardRulingDTO>> getRulings(
            @PathVariable("id") String id
    ) {
        return cardCatalogService.getCardById(UUID.fromString(id))
                .map(card -> {
                    UUID oracleId = UUID.fromString(card.oracleId());
                    return ResponseEntity.ok(cardCatalogService.getRulings(oracleId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Manual sync ──────────────────────────────────────────────────────────

    /**
     * Trigger a manual Scryfall bulk-data sync.
     *
     * <p>The sync runs asynchronously; this endpoint returns {@code 202 Accepted}
     * immediately. Monitor application logs for progress.
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> triggerSync() {
        log.info("Manual Scryfall sync requested via API");
        runSyncAsync();
        return ResponseEntity.accepted().build();
    }

    @Async
    protected void runSyncAsync() {
        scryfallBulkSyncJob.run();
    }

    // ─── Sets (stub) ──────────────────────────────────────────────────────────

    /**
     * Return a list of all known sets.
     *
     * <p><strong>TODO:</strong> implement once a {@code sets} table or a
     * dedicated {@code Set} entity is added. Currently returns an empty list.
     */
    @GetMapping("/sets")
    public ResponseEntity<List<Object>> getSets() {
        // TODO: implement with a Set entity / repository
        return ResponseEntity.ok(Collections.emptyList());
    }
}
