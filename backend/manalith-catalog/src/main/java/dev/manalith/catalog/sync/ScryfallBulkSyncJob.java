package dev.manalith.catalog.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ScryfallBulkSyncJob
 *
 * Runs on a nightly cron (default 4 AM, configurable via manalith.scryfall.sync-cron).
 *
 * Steps:
 *  1. GET https://api.scryfall.com/bulk-data — fetch metadata
 *  2. Resolve download_uri for "default_cards"
 *  3. Download JSON to temp file, validate checksum
 *  4. Stream-parse JSON array and upsert into card_printings
 *  5. Download + upsert rulings bulk file
 *  6. Emit ScryfallSyncCompletedEvent for downstream listeners
 *
 * Rate limiting: only one HTTP call for the metadata + one download per run.
 * No per-card API calls; all data comes from bulk files.
 */
@Component
public class ScryfallBulkSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ScryfallBulkSyncJob.class);

    // TODO: inject ScryfallClient, CardPrintingRepository, ApplicationEventPublisher

    @Scheduled(cron = "${manalith.scryfall.sync-cron:0 0 4 * * *}")
    public void run() {
        log.info("Starting Scryfall bulk sync...");
        // TODO: implement steps above
        log.info("Scryfall bulk sync complete.");
    }
}
