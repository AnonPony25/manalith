package dev.manalith.deck.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.deck.dto.DeckStatsDTO;
import dev.manalith.deck.dto.LegalityResultDTO;
import dev.manalith.deck.model.DeckEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckStatsService {

    private final ObjectMapper objectMapper;

    private static final List<String> TYPE_ORDER = List.of(
            "Creature", "Instant", "Sorcery", "Enchantment", "Artifact", "Planeswalker", "Land"
    );

    /**
     * Compute deck statistics. The {@code legality} field in the returned DTO is left null;
     * the caller (DeckService) is responsible for assembling it via DeckLegalityValidator.
     */
    public DeckStatsDTO computeStats(List<DeckEntry> mainboard, List<DeckEntry> sideboard, String format) {
        // -----------------------------------------------------------------------
        // Totals
        // -----------------------------------------------------------------------
        int totalCards = mainboard.stream().mapToInt(DeckEntry::getQuantity).sum();
        int uniqueCards = mainboard.size();
        int sideboardCount = sideboard.stream().mapToInt(DeckEntry::getQuantity).sum();

        // -----------------------------------------------------------------------
        // Color distribution (mainboard only)
        // -----------------------------------------------------------------------
        Map<String, Integer> colorDistribution = new LinkedHashMap<>();
        for (String c : List.of("W", "U", "B", "R", "G", "C")) {
            colorDistribution.put(c, 0);
        }

        for (DeckEntry entry : mainboard) {
            List<String> colors = parseJsonStringArray(entry.getCard().getColorsJson());
            if (colors.isEmpty()) {
                colorDistribution.merge("C", entry.getQuantity(), Integer::sum);
            } else {
                for (String color : colors) {
                    colorDistribution.merge(color, entry.getQuantity(), Integer::sum);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Mana curve (non-land mainboard cards)
        // -----------------------------------------------------------------------
        Map<String, Integer> manaCurve = new LinkedHashMap<>();
        for (String key : List.of("0", "1", "2", "3", "4", "5", "6", "7+")) {
            manaCurve.put(key, 0);
        }

        // -----------------------------------------------------------------------
        // Type breakdown
        // -----------------------------------------------------------------------
        Map<String, Integer> typeBreakdown = new LinkedHashMap<>();
        for (String t : TYPE_ORDER) {
            typeBreakdown.put(t, 0);
        }
        typeBreakdown.put("Other", 0);

        // -----------------------------------------------------------------------
        // Average CMC (non-land mainboard)
        // -----------------------------------------------------------------------
        BigDecimal totalCmcSum = BigDecimal.ZERO;
        int nonLandCount = 0;

        for (DeckEntry entry : mainboard) {
            String typeLine = entry.getCard().getTypeLine() != null ? entry.getCard().getTypeLine() : "";
            boolean isLand = typeLine.contains("Land");

            // Type breakdown
            String matchedType = "Other";
            for (String t : TYPE_ORDER) {
                if (typeLine.contains(t)) {
                    matchedType = t;
                    break;
                }
            }
            typeBreakdown.merge(matchedType, entry.getQuantity(), Integer::sum);

            // Mana curve & average CMC (skip lands)
            if (!isLand) {
                BigDecimal cmc = entry.getCard().getCmc() != null ? entry.getCard().getCmc() : BigDecimal.ZERO;
                int cmcInt = cmc.intValue();
                String bucket;
                if (cmcInt >= 7) {
                    bucket = "7+";
                } else {
                    bucket = String.valueOf(cmcInt);
                }
                manaCurve.merge(bucket, entry.getQuantity(), Integer::sum);

                // Average CMC accumulation
                totalCmcSum = totalCmcSum.add(cmc.multiply(BigDecimal.valueOf(entry.getQuantity())));
                nonLandCount += entry.getQuantity();
            }
        }

        double averageCmc = 0.0;
        if (nonLandCount > 0) {
            averageCmc = totalCmcSum
                    .divide(BigDecimal.valueOf(nonLandCount), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // -----------------------------------------------------------------------
        // Color identity (union of all mainboard + commander entries)
        // -----------------------------------------------------------------------
        Set<String> colorIdentitySet = new LinkedHashSet<>();
        List<DeckEntry> all = new ArrayList<>(mainboard);
        // commanders come from mainboard list (isCommander flag) — already included
        for (DeckEntry entry : all) {
            colorIdentitySet.addAll(parseJsonStringArray(entry.getCard().getColorIdentityJson()));
        }
        List<String> colorIdentity = new ArrayList<>(colorIdentitySet);

        return new DeckStatsDTO(
                totalCards,
                uniqueCards,
                sideboardCount,
                colorDistribution,
                manaCurve,
                typeBreakdown,
                averageCmc,
                colorIdentity,
                null // legality assembled by DeckService
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<String> parseJsonStringArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON string array: {}", json, e);
            return List.of();
        }
    }
}
