package dev.manalith.catalog.sync;

/**
 * Thrown when the Scryfall API returns a non-2xx response or an I/O error
 * occurs during a request.
 */
public class ScryfallApiException extends RuntimeException {

    public ScryfallApiException(String message) {
        super(message);
    }

    public ScryfallApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
