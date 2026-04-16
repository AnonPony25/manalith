package dev.manalith.catalog.sync;

import dev.manalith.catalog.event.ScryfallSyncCompletedEvent;
import dev.manalith.catalog.repository.CardPrintingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Nightly Scryfall bulk-data sync job.
 *
 * <p>Runs on a configurable cron expression (default: 04:00 server time daily).
 * On each run:
 * <ol>
 *   <li>Fetches the {@code /bulk-data} metadata listing from Scryfall.</li>
 *   <li>Finds the {@code default_cards} entry and compares its {@code updated_at}
 *       timestamp against a marker file written after the previous sync.</li>
 *   <li>If the remote file is newer, downloads it to a temp path and delegates
 *       to {@link ScryfallCardImporter} for stream-parsing and upsert.</li>
 *   <li>Repeats the same flow for the {@code rulings} bulk file.</li>
 *   <li>Publishes a {@link ScryfallSyncCompletedEvent} for downstream listeners.</li>
 * </ol>
 *
 * <p>On application startup, {@link #syncOnStartup()} fires automatically if the
 * {@code card_printings} table is empty so that a fresh deployment is immediately
 * populated without waiting for the next scheduled run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScryfallBulkSyncJob {

    /** File path used to persist the timestamp of the last successful sync. */
    private static final Path LAST_SYNC_MARKER =
            Paths.get(System.getProperty("java.io.tmpdir"), "manalith-last-scryfall-sync");

    private final ScryfallApiClient scryfallApiClient;
    private final ScryfallCardImporter scryfallCardImporter;
    private final ApplicationEventPublisher eventPublisher;
    private final CardPrintingRepository cardPrintingRepository;

    // ─── Scheduled entry point ────────────────────────────────────────────────

    /**
     * Nightly sync — cron configurable via {@code manalith.scryfall.sync-cron}.
     */
    @Scheduled(cron = "${manalith.scryfall.sync-cron:0 0 4 * * *}")
    public void run() {
        log.info("Starting Scryfall bulk sync...");
        int cardsUpserted = 0;
        int rulingsUpserted = 0;

        try {
            // Step 1: Fetch metadata
            ScryfallApiClient.ScryfallBulkDataResponse metadata =
                    scryfallApiClient.getBulkDataMetadata();

            // Step 2: Sync default_cards
            ScryfallApiClient.ScryfallBulkDataEntry cardsEntry = findEntry(metadata, "default_cards");
            if (cardsEntry != null && isNewer(cardsEntry.updatedAt())) {
                cardsUpserted = syncBulkFile(cardsEntry, "default_cards.json", false);
            } else {
                log.info("default_cards bulk file is up-to-date; skipping download.");
            }

            // Step 3: Sync rulings
            ScryfallApiClient.ScryfallBulkDataEntry rulingsEntry = findEntry(metadata, "rulings");
            if (rulingsEntry != null && isNewer(rulingsEntry.updatedAt())) {
                rulingsUpserted = syncBulkFile(rulingsEntry, "scryfall-rulings.json", true);
            } else {
                log.info("rulings bulk file is up-to-date; skipping download.");
            }

            // Step 4: Update sync marker
            writeSyncMarker();

            // Step 5: Publish completion event
            eventPublisher.publishEvent(
                    new ScryfallSyncCompletedEvent(Instant.now(), cardsUpserted, rulingsUpserted));

            log.info("Scryfall bulk sync complete. cards={} rulings={}", cardsUpserted, rulingsUpserted);

        } catch (ScryfallApiException e) {
            log.error("Scryfall sync failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during Scryfall sync", e);
        }
    }

    // ─── Startup sync ─────────────────────────────────────────────────────────

    /**
     * Trigger a full sync if the database has no cards yet.
     * Called once immediately after the Spring context is initialised.
     */
    @PostConstruct
    public void syncOnStartup() {
        if (cardPrintingRepository.count() == 0) {
            log.info("card_printings table is empty — triggering initial Scryfall sync on startup.");
            run();
        } else {
            log.info("card_printings already populated ({} rows); skipping startup sync.",
                    cardPrintingRepository.count());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Download the bulk file for {@code entry} to a temporary path, then invoke
     * the appropriate importer method.
     *
     * @param entry    the Scryfall bulk-data metadata entry
     * @param filename suggested temp file name (for log clarity)
     * @param rulings  {@code true} to invoke the rulings importer; {@code false} for cards
     * @return number of entities upserted
     */
    private int syncBulkFile(ScryfallApiClient.ScryfallBulkDataEntry entry,
                             String filename,
                             boolean rulings) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile("manalith-scryfall-", "-" + filename);
        } catch (IOException e) {
            throw new ScryfallApiException("Cannot create temp file for bulk download", e);
        }

        try {
            log.info("Downloading {} ({} bytes)...", filename, entry.size());
            scryfallApiClient.downloadBulkFile(entry.downloadUri(), tempFile);

            ScryfallCardImporter.ImportResult result = rulings
                    ? scryfallCardImporter.importRulingsFromFile(tempFile)
                    : scryfallCardImporter.importFromFile(tempFile);

            log.info("{} import finished: upserted={} errors={} elapsed={}",
                    filename, result.cardsUpserted(), result.errors(), result.elapsed());
            return result.cardsUpserted();

        } finally {
            // Best-effort cleanup of temp file
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                log.debug("Could not delete temp file {}", tempFile);
            }
        }
    }

    /**
     * Find a bulk-data entry by type name (e.g. {@code "default_cards"}).
     */
    private static ScryfallApiClient.ScryfallBulkDataEntry findEntry(
            ScryfallApiClient.ScryfallBulkDataResponse metadata, String type) {
        return metadata.data().stream()
                .filter(e -> type.equals(e.type()))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("No bulk-data entry found for type '{}'", type);
                    return null;
                });
    }

    /**
     * Returns {@code true} if {@code updatedAt} is after the last recorded sync time.
     * Always returns {@code true} if the marker file does not exist.
     */
    private boolean isNewer(String updatedAt) {
        if (updatedAt == null) return true;
        if (!Files.exists(LAST_SYNC_MARKER)) return true;
        try {
            String markerContent = Files.readString(LAST_SYNC_MARKER).trim();
            Instant lastSync = Instant.parse(markerContent);
            Instant remoteUpdate = OffsetDateTime.parse(updatedAt).toInstant();
            return remoteUpdate.isAfter(lastSync);
        } catch (IOException | DateTimeParseException e) {
            log.warn("Could not read sync marker; treating as stale: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Write the current timestamp to the sync marker file.
     */
    private void writeSyncMarker() {
        try {
            Files.writeString(LAST_SYNC_MARKER, Instant.now().toString());
        } catch (IOException e) {
            log.warn("Failed to write sync marker to {}: {}", LAST_SYNC_MARKER, e.getMessage());
        }
    }
}
