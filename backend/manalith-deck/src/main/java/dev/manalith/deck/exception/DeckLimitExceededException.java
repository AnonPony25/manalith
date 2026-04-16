package dev.manalith.deck.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class DeckLimitExceededException extends RuntimeException {
    public DeckLimitExceededException(String message) {
        super(message);
    }

    public DeckLimitExceededException(int limit) {
        super("Deck limit of " + limit + " reached. Please delete an existing deck before creating a new one.");
    }
}
