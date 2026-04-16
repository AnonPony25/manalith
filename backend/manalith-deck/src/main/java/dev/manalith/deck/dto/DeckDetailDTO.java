package dev.manalith.deck.dto;

import java.util.List;

public record DeckDetailDTO(
        String id,
        String ownerId,
        String ownerName,
        String name,
        String format,
        String description,
        boolean isPublic,
        String createdAt,
        String updatedAt,
        List<DeckEntryDTO> mainboard,
        List<DeckEntryDTO> sideboard,
        List<DeckEntryDTO> commanders,
        DeckStatsDTO stats
) {}
