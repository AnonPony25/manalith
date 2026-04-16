package dev.manalith.deck.dto;

import java.util.List;
import java.util.Map;

public record DeckStatsDTO(
        int totalCards,
        int uniqueCards,
        int sideboardCount,
        Map<String, Integer> colorDistribution,
        Map<String, Integer> manaCurve,
        Map<String, Integer> typeBreakdown,
        double averageCmc,
        List<String> colorIdentity,
        LegalityResultDTO legality
) {}
