package dev.manalith.deck.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a deck import operation fails due to malformed input or too many
 * unresolvable card names.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DeckImportException extends RuntimeException {

    public DeckImportException(String message) {
        super(message);
    }

    public DeckImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
