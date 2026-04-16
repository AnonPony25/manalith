package dev.manalith.catalog.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.model.CardRuling;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.catalog.repository.CardRulingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Streams a Scryfall bulk JSON file from disk and upserts its contents into
 * the {@code card_printings} and {@code card_rulings} tables.
 *
 * <p>Jackson's {@code MappingIterator} is used to parse the JSON array
 * incrementally, keeping heap usage constant for the ~300 MB default-cards file.
 * Cards are accumulated in a buffer and flushed to the DB in batches of 500
 * to minimise round-trips while avoiding excessive memory use.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScryfallCardImporter {

    private static final int CARD_BATCH_SIZE = 500;
    private static final int LOG_INTERVAL = 1_000;

    private final CardPrintingRepository cardPrintingRepository;
    private final CardRulingRepository cardRulingRepository;
    private final ObjectMapper objectMapper;

    // ─── Result record ────────────────────────────────────────────────────────

    /**
     * Summary of a completed import run.
     *
     * @param cardsUpserted total entities saved (new + updated)
     * @param errors        number of cards/rulings that failed to parse or save
     * @param elapsed       wall-clock duration of the import
     */
    public record ImportResult(int cardsUpserted, int errors, Duration elapsed) {}

    // ─── Card import ──────────────────────────────────────────────────────────

    /**
     * Stream-parse a Scryfall {@code default_cards} bulk JSON file and upsert
     * all card printings into the database.
     *
     * @param jsonFile path to the downloaded JSON file
     * @return import statistics
     */
    @Transactional
    public ImportResult importFromFile(Path jsonFile) {
        log.info("Starting card import from {}", jsonFile);
        Instant start = Instant.now();
        int total = 0;
        int errors = 0;
        List<CardPrinting> buffer = new ArrayList<>(CARD_BATCH_SIZE);

        try (MappingIterator<Map<String, Object>> it =
                     objectMapper.readerFor(new TypeReference<Map<String, Object>>() {})
                             .readValues(jsonFile.toFile())) {

            while (it.hasNextValue()) {
                Map<String, Object> raw;
                try {
                    raw = it.nextValue();
                } catch (Exception e) {
                    log.warn("Failed to read card entry: {}", e.getMessage());
                    errors++;
                    continue;
                }

                try {
                    buffer.add(mapCard(raw));
                } catch (Exception e) {
                    log.warn("Failed to map card '{}': {}", raw.get("name"), e.getMessage());
                    errors++;
                    continue;
                }

                total++;

                if (buffer.size() >= CARD_BATCH_SIZE) {
                    cardPrintingRepository.saveAll(buffer);
                    buffer.clear();
                }

                if (total % LOG_INTERVAL == 0) {
                    log.info("Card import progress: {} cards processed, {} errors", total, errors);
                }
            }

            // Flush remaining buffer
            if (!buffer.isEmpty()) {
                cardPrintingRepository.saveAll(buffer);
            }

        } catch (Exception e) {
            log.error("Fatal error during card import from {}: {}", jsonFile, e.getMessage(), e);
            throw new ScryfallApiException("Card import failed", e);
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Card import complete: {} upserted, {} errors, elapsed={}", total, errors, elapsed);
        return new ImportResult(total, errors, elapsed);
    }

    // ─── Rulings import ───────────────────────────────────────────────────────

    /**
     * Stream-parse a Scryfall {@code rulings} bulk JSON file and upsert
     * all rulings into the database.
     *
     * <p>Existing rulings for each oracle ID are deleted before inserting the
     * new batch to avoid duplicates (Scryfall does not provide stable ruling IDs).
     *
     * @param jsonFile path to the downloaded rulings JSON file
     * @return import statistics
     */
    @Transactional
    public ImportResult importRulingsFromFile(Path jsonFile) {
        log.info("Starting rulings import from {}", jsonFile);
        Instant start = Instant.now();
        int total = 0;
        int errors = 0;
        List<CardRuling> buffer = new ArrayList<>(CARD_BATCH_SIZE);

        try (MappingIterator<Map<String, Object>> it =
                     objectMapper.readerFor(new TypeReference<Map<String, Object>>() {})
                             .readValues(jsonFile.toFile())) {

            while (it.hasNextValue()) {
                Map<String, Object> raw;
                try {
                    raw = it.nextValue();
                } catch (Exception e) {
                    log.warn("Failed to read ruling entry: {}", e.getMessage());
                    errors++;
                    continue;
                }

                try {
                    buffer.add(mapRuling(raw));
                } catch (Exception e) {
                    log.warn("Failed to map ruling: {}", e.getMessage());
                    errors++;
                    continue;
                }

                total++;

                if (buffer.size() >= CARD_BATCH_SIZE) {
                    flushRulings(buffer);
                    buffer.clear();
                }

                if (total % LOG_INTERVAL == 0) {
                    log.info("Rulings import progress: {} processed, {} errors", total, errors);
                }
            }

            if (!buffer.isEmpty()) {
                flushRulings(buffer);
            }

        } catch (Exception e) {
            log.error("Fatal error during rulings import from {}: {}", jsonFile, e.getMessage(), e);
            throw new ScryfallApiException("Rulings import failed", e);
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Rulings import complete: {} upserted, {} errors, elapsed={}", total, errors, elapsed);
        return new ImportResult(total, errors, elapsed);
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private CardPrinting mapCard(Map<String, Object> raw) throws Exception {
        String idStr = (String) raw.get("id");
        String oracleIdStr = (String) raw.get("oracle_id");

        // Re-serialise collection fields back to JSON strings for JSONB storage
        String colorsJson = toJson(raw.get("colors"));
        String colorIdentityJson = toJson(raw.get("color_identity"));
        String legalitiesJson = toJson(raw.get("legalities"));
        String imageUrisJson = toJson(raw.get("image_uris"));
        String cardFacesJson = toJson(raw.get("card_faces"));
        String pricesJson = toJson(raw.get("prices"));

        Object cmcRaw = raw.get("cmc");
        BigDecimal cmc = cmcRaw != null
                ? new BigDecimal(cmcRaw.toString())
                : BigDecimal.ZERO;

        String releasedAtStr = (String) raw.get("released_at");
        LocalDate releasedAt = releasedAtStr != null ? LocalDate.parse(releasedAtStr) : null;

        String updatedAtStr = (String) raw.get("updated_at");
        Instant updatedAt = updatedAtStr != null ? Instant.parse(updatedAtStr) : Instant.now();

        return CardPrinting.builder()
                .id(idStr != null ? UUID.fromString(idStr) : null)
                .oracleId(oracleIdStr != null ? UUID.fromString(oracleIdStr) : null)
                .name(stringOf(raw, "name"))
                .setCode(stringOf(raw, "set"))
                .setName(stringOf(raw, "set_name"))
                .collectorNumber(stringOf(raw, "collector_number"))
                .rarity(stringOf(raw, "rarity"))
                .layout(stringOf(raw, "layout"))
                .manaCost(stringOf(raw, "mana_cost"))
                .cmc(cmc)
                .typeLine(stringOf(raw, "type_line"))
                .oracleText(stringOf(raw, "oracle_text"))
                .colorsJson(colorsJson)
                .colorIdentityJson(colorIdentityJson)
                .legalitiesJson(legalitiesJson)
                .imageUrisJson(imageUrisJson)
                .cardFacesJson(cardFacesJson)
                .pricesJson(pricesJson)
                .releasedAt(releasedAt)
                .updatedAt(updatedAt)
                .build();
    }

    private CardRuling mapRuling(Map<String, Object> raw) {
        String oracleIdStr = (String) raw.get("oracle_id");
        String publishedAtStr = (String) raw.get("published_at");
        return CardRuling.builder()
                .oracleId(oracleIdStr != null ? UUID.fromString(oracleIdStr) : null)
                .source(stringOf(raw, "source"))
                .publishedAt(publishedAtStr != null ? LocalDate.parse(publishedAtStr) : null)
                .comment(stringOf(raw, "comment"))
                .build();
    }

    /**
     * Delete existing rulings for every unique oracle ID in the batch,
     * then bulk-insert the new ones.
     */
    private void flushRulings(List<CardRuling> batch) {
        batch.stream()
                .map(CardRuling::getOracleId)
                .distinct()
                .forEach(cardRulingRepository::deleteByOracleId);
        cardRulingRepository.saveAll(batch);
    }

    private String toJson(Object value) throws Exception {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }

    private static String stringOf(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
