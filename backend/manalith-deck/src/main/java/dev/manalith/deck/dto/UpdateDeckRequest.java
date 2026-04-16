package dev.manalith.deck.dto;

public record UpdateDeckRequest(
        String name,
        String format,
        String description,
        Boolean isPublic
) {}
