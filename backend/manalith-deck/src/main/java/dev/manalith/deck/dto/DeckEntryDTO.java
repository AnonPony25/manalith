package dev.manalith.deck.dto;

public record DeckEntryDTO(
        String cardId,
        String cardName,
        String manaCost,
        double cmc,
        String typeLine,
        String setCode,
        String rarity,
        String imageUri,
        int quantity,
        boolean isCommander
) {}
