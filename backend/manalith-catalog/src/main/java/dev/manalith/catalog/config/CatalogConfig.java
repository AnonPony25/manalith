package dev.manalith.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the catalog module.
 *
 * <ul>
 *   <li>Registers a pre-configured {@link RestClient} bean for Scryfall API calls.</li>
 *   <li>Enables Spring's asynchronous task execution ({@link EnableAsync}).</li>
 *   <li>Enables scheduled task support ({@link EnableScheduling}) for the nightly sync.</li>
 * </ul>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class CatalogConfig {

    @Value("${manalith.scryfall.base-url:https://api.scryfall.com}")
    private String scryfallBaseUrl;

    @Value("${manalith.scryfall.user-agent:Manalith/0.1 (+https://github.com/manalith/manalith)}")
    private String scryfallUserAgent;

    /**
     * A {@link RestClient} pre-configured with the Scryfall base URL and
     * the required {@code User-Agent} header.
     *
     * <p>Per Scryfall's API policy, a meaningful User-Agent string must be
     * sent with every request to identify the application and provide a
     * contact point. Automated tools that do not send a User-Agent may be
     * rate-limited or blocked.
     *
     * @param builder the auto-configured {@link RestClient.Builder} provided by Spring Boot
     */
    @Bean
    public RestClient scryfallRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(scryfallBaseUrl)
                .defaultHeader("User-Agent", scryfallUserAgent)
                .defaultHeader("Accept", "application/json;q=0.9,*/*;q=0.8")
                .build();
    }
}
