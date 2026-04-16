package dev.manalith.deck.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddCardRequest(
        @NotBlank
        String cardId,

        @Min(1) @Max(99)
        int quantity,

        boolean isCommander,

        boolean isSideboard
) {}
