package dev.manalith.catalog.event;

import java.time.Instant;

/**
 * Spring application event published after a successful Scryfall bulk sync run.
 *
 * <p>Consumers (e.g. price-alert services, search index rebuilders) can listen
 * for this event via {@code @EventListener} without coupling to the sync job.
 *
 * @param completedAt  timestamp when the sync finished
 * @param cardsUpserted number of card printings inserted or updated
 * @param rulingsUpserted number of rulings inserted or updated
 */
public record ScryfallSyncCompletedEvent(
        Instant completedAt,
        int cardsUpserted,
        int rulingsUpserted
) {}
