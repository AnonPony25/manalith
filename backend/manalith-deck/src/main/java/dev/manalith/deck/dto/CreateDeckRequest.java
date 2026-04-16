package dev.manalith.deck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDeckRequest(
        @NotBlank
        String name,

        @NotBlank
        @Pattern(regexp = "standard|pioneer|modern|legacy|vintage|commander|pauper|draft|sealed|custom")
        String format,

        String description,

        boolean isPublic
) {}
