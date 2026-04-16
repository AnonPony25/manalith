package dev.manalith.deck.dto;

public record DeckSummaryDTO(
        String id,
        String ownerId,
        String ownerName,
        String name,
        String format,
        String description,
        boolean isPublic,
        int mainboardCount,
        int sideboardCount,
        String createdAt,
        String updatedAt
) {}
