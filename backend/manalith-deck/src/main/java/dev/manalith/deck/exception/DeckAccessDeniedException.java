package dev.manalith.deck.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DeckAccessDeniedException extends RuntimeException {
    public DeckAccessDeniedException(String message) {
        super(message);
    }

    public DeckAccessDeniedException() {
        super("Access to this deck is denied.");
    }
}
