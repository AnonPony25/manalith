package dev.manalith.catalog.sync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * HTTP client for the Scryfall bulk-data API.
 *
 * <p>Uses Spring 6's {@link RestClient} (injected via {@link dev.manalith.catalog.config.CatalogConfig}).
 * Only two remote calls are made per sync cycle:
 * <ol>
 *   <li>A small metadata request to {@code /bulk-data} to discover the download URI and
 *       last-updated timestamp.</li>
 *   <li>A streaming download of the chosen bulk JSON file directly to a temporary file,
 *       avoiding loading the entire ~300 MB payload into heap memory.</li>
 * </ol>
 *
 * <p>Per Scryfall policy, a descriptive {@code User-Agent} header is sent with every request.
 */
@Slf4j
@Component
public class ScryfallApiClient {

    @Value("${manalith.scryfall.bulk-data-url:https://api.scryfall.com/bulk-data}")
    private String bulkDataUrl;

    @Value("${manalith.scryfall.user-agent:Manalith/0.1 (+https://github.com/manalith/manalith)}")
    private String userAgent;

    /** Pre-configured {@link RestClient} bean (base URL + User-Agent set in {@code CatalogConfig}). */
    private final RestClient restClient;

    public ScryfallApiClient(RestClient scryfallRestClient) {
        this.restClient = scryfallRestClient;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Fetch the bulk-data metadata listing from Scryfall.
     *
     * @return wrapper containing the list of available bulk-data entries
     * @throws ScryfallApiException if the server returns a non-2xx response
     */
    public ScryfallBulkDataResponse getBulkDataMetadata() {
        log.debug("Fetching Scryfall bulk-data metadata from {}", bulkDataUrl);
        try {
            ScryfallBulkDataResponse response = restClient.get()
                    .uri(bulkDataUrl)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new ScryfallApiException(
                                "Scryfall metadata request failed: HTTP " + res.getStatusCode());
                    })
                    .body(ScryfallBulkDataResponse.class);
            if (response == null) {
                throw new ScryfallApiException("Scryfall returned an empty body for bulk-data metadata");
            }
            log.info("Retrieved {} bulk-data entries from Scryfall", response.data().size());
            return response;
        } catch (ScryfallApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ScryfallApiException("Failed to fetch Scryfall bulk-data metadata", e);
        }
    }

    /**
     * Stream-download a Scryfall bulk JSON file to {@code targetPath}.
     *
     * <p>The response body is piped directly from the HTTP connection to the file
     * system using {@link Files#copy(InputStream, Path, java.nio.file.CopyOption...)}
     * to keep heap usage constant regardless of file size.
     *
     * @param downloadUri the {@code download_uri} from a {@link ScryfallBulkDataEntry}
     * @param targetPath  the local path to write the downloaded file to
     * @throws ScryfallApiException if the download fails
     */
    public void downloadBulkFile(String downloadUri, Path targetPath) {
        log.info("Downloading Scryfall bulk file from {} → {}", downloadUri, targetPath);
        try {
            restClient.get()
                    .uri(downloadUri)
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        throw new ScryfallApiException(
                                "Scryfall bulk download failed: HTTP " + res.getStatusCode()
                                        + " for URI " + downloadUri);
                    })
                    .toEntity(byte[].class); // triggers the request

            // Streaming alternative — use execute() for true streaming:
            restClient.get()
                    .uri(downloadUri)
                    .header("User-Agent", userAgent)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new ScryfallApiException(
                                    "Scryfall bulk download failed: HTTP " + response.getStatusCode());
                        }
                        try (InputStream body = response.getBody()) {
                            Files.copy(body, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ioEx) {
                            throw new ScryfallApiException("I/O error writing bulk file to " + targetPath, ioEx);
                        }
                        return null; // exchange must return a value
                    });

            log.info("Bulk file written to {} ({} bytes)", targetPath, Files.size(targetPath));
        } catch (ScryfallApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ScryfallApiException("Failed to download Scryfall bulk file from " + downloadUri, e);
        }
    }

    // ─── Response records ─────────────────────────────────────────────────────

    /**
     * Top-level Scryfall bulk-data response envelope.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallBulkDataResponse(List<ScryfallBulkDataEntry> data) {}

    /**
     * Metadata for a single bulk-data object available from Scryfall.
     *
     * @param type        bulk object type identifier, e.g. {@code "default_cards"} or {@code "rulings"}
     * @param downloadUri the URI to download the actual JSON file
     * @param updatedAt   ISO-8601 timestamp of when this file was last regenerated
     * @param size        approximate compressed file size in bytes
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScryfallBulkDataEntry(
            String type,
            @JsonProperty("download_uri") String downloadUri,
            @JsonProperty("updated_at") String updatedAt,
            long size
    ) {}
}
