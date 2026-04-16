package dev.manalith.deck.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Manalith Deck module.
 *
 * <p>Future deck-specific beans (e.g. custom Jackson modules, caching setup,
 * format-validation strategies) belong here.
 *
 * <p>The {@link com.fasterxml.jackson.databind.ObjectMapper} used by
 * {@link dev.manalith.deck.service.DeckExportService} is intentionally sourced
 * from the shared Spring application context (auto-configured by Spring Boot)
 * rather than defining a separate bean here, to avoid ambiguity.
 */
@Configuration
public class DeckModuleConfig {
    // No beans defined yet — placeholder for future deck-specific configuration.
}
