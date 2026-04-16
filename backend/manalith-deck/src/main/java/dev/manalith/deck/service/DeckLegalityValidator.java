package dev.manalith.deck.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.deck.dto.LegalityResultDTO;
import dev.manalith.deck.model.Deck;
import dev.manalith.deck.model.DeckEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeckLegalityValidator {

    private final ObjectMapper objectMapper;

    public List<String> getSupportedFormats() {
        return List.of("standard", "pioneer", "modern", "legacy", "vintage", "commander",
                "pauper", "draft", "sealed", "custom");
    }

    public LegalityResultDTO validate(Deck deck, String format) {
        if (format == null || format.isBlank()) {
            format = deck.getFormat();
        }
        List<DeckEntry> entries = deck.getEntries();
        List<String> violations = new ArrayList<>();

        switch (format.toLowerCase()) {
            case "commander" -> validateCommander(entries, violations);
            case "standard", "pioneer", "modern", "legacy", "vintage", "pauper" ->
                    validateConstructed(entries, format.toLowerCase(), violations);
            case "draft", "sealed" -> validateLimitedFormat(entries, violations);
            case "custom" -> { /* always legal */ }
            default -> violations.add("Unknown format: " + format);
        }

        if (violations.isEmpty()) {
            return LegalityResultDTO.legal(format);
        } else {
            return LegalityResultDTO.illegal(format, violations);
        }
    }

    // -----------------------------------------------------------------------
    // Commander
    // -----------------------------------------------------------------------

    private void validateCommander(List<DeckEntry> entries, List<String> violations) {
        List<DeckEntry> commanderEntries = entries.stream()
                .filter(DeckEntry::isCommander)
                .toList();
        List<DeckEntry> mainboard = entries.stream()
                .filter(e -> !e.isSideboard())
                .toList();

        // Exactly 1 commander
        if (commanderEntries.isEmpty()) {
            violations.add("Commander format requires exactly 1 commander entry marked as commander.");
        } else if (commanderEntries.size() > 1) {
            violations.add("Commander format allows only 1 commander, but found " + commanderEntries.size() + ".");
        }

        // 100 cards total (including commander)
        int totalCards = mainboard.stream().mapToInt(DeckEntry::getQuantity).sum();
        if (totalCards != 100) {
            violations.add("Commander decks must have exactly 100 cards (including commander); found " + totalCards + ".");
        }

        // No duplicates except basic lands
        Map<String, Integer> oracleIdCounts = new HashMap<>();
        for (DeckEntry entry : mainboard) {
            String oracleId = entry.getCard().getOracleId() != null
                    ? entry.getCard().getOracleId().toString()
                    : entry.getCard().getId().toString();
            String typeLine = entry.getCard().getTypeLine() != null ? entry.getCard().getTypeLine() : "";
            boolean isBasicLand = typeLine.contains("Basic Land");
            if (!isBasicLand) {
                oracleIdCounts.merge(oracleId, entry.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : oracleIdCounts.entrySet()) {
            if (e.getValue() > 1) {
                violations.add("Duplicate non-basic card (oracleId=" + e.getKey() + ") found " + e.getValue() + " times; Commander allows only 1 copy.");
            }
        }

        // Commander color identity must contain all card color identities
        if (!commanderEntries.isEmpty()) {
            Set<String> commanderColorIdentity = new HashSet<>();
            for (DeckEntry ce : commanderEntries) {
                commanderColorIdentity.addAll(parseJsonStringArray(ce.getCard().getColorIdentityJson()));
            }

            for (DeckEntry entry : mainboard) {
                if (entry.isCommander()) continue;
                List<String> cardColorIdentity = parseJsonStringArray(entry.getCard().getColorIdentityJson());
                for (String color : cardColorIdentity) {
                    if (!commanderColorIdentity.contains(color)) {
                        violations.add("Card '" + entry.getCard().getName() + "' has color identity [" + color
                                + "] outside commander's color identity.");
                    }
                }
            }
        }

        // Each card must be legal or restricted in commander
        for (DeckEntry entry : mainboard) {
            String legality = getLegalityForFormat(entry.getCard().getLegalitiesJson(), "commander");
            if (!"legal".equals(legality) && !"restricted".equals(legality)) {
                violations.add("Card '" + entry.getCard().getName() + "' is " + legality + " in Commander.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Constructed (standard, pioneer, modern, legacy, vintage, pauper)
    // -----------------------------------------------------------------------

    private void validateConstructed(List<DeckEntry> entries, String format, List<String> violations) {
        List<DeckEntry> mainboard = entries.stream()
                .filter(e -> !e.isSideboard())
                .toList();
        List<DeckEntry> sideboard = entries.stream()
                .filter(DeckEntry::isSideboard)
                .toList();

        // 60 card minimum mainboard
        int mainboardTotal = mainboard.stream().mapToInt(DeckEntry::getQuantity).sum();
        if (mainboardTotal < 60) {
            violations.add("Mainboard must have at least 60 cards; found " + mainboardTotal + ".");
        }

        // Max 15 sideboard
        int sideboardTotal = sideboard.stream().mapToInt(DeckEntry::getQuantity).sum();
        if (sideboardTotal > 15) {
            violations.add("Sideboard may not exceed 15 cards; found " + sideboardTotal + ".");
        }

        // Max 4 copies of any non-basic-land card
        Map<String, Integer> oracleIdCounts = new HashMap<>();
        Map<String, String> oracleIdToName = new HashMap<>();
        for (DeckEntry entry : entries) {
            String typeLine = entry.getCard().getTypeLine() != null ? entry.getCard().getTypeLine() : "";
            boolean isBasicLand = typeLine.contains("Basic Land");
            if (!isBasicLand) {
                String oracleId = entry.getCard().getOracleId() != null
                        ? entry.getCard().getOracleId().toString()
                        : entry.getCard().getId().toString();
                oracleIdCounts.merge(oracleId, entry.getQuantity(), Integer::sum);
                oracleIdToName.putIfAbsent(oracleId, entry.getCard().getName());
            }
        }
        for (Map.Entry<String, Integer> e : oracleIdCounts.entrySet()) {
            if (e.getValue() > 4) {
                violations.add("Card '" + oracleIdToName.get(e.getKey()) + "' appears " + e.getValue()
                        + " times (max 4 for non-basic-land cards in " + format + ").");
            }
        }

        // Format legality check for each card
        for (DeckEntry entry : entries) {
            String legality = getLegalityForFormat(entry.getCard().getLegalitiesJson(), format);
            if ("banned".equals(legality)) {
                violations.add("Card '" + entry.getCard().getName() + "' is banned in " + format + ".");
            } else if ("not_legal".equals(legality)) {
                violations.add("Card '" + entry.getCard().getName() + "' is not legal in " + format + ".");
            }
            // "restricted" allows 1 copy — enforce that
            if ("restricted".equals(legality)) {
                String oracleId = entry.getCard().getOracleId() != null
                        ? entry.getCard().getOracleId().toString()
                        : entry.getCard().getId().toString();
                int count = oracleIdCounts.getOrDefault(oracleId, entry.getQuantity());
                if (count > 1) {
                    violations.add("Card '" + entry.getCard().getName() + "' is restricted in " + format + " (max 1 copy allowed).");
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Limited (draft, sealed): 40 card minimum; no legality restrictions
    // -----------------------------------------------------------------------

    private void validateLimitedFormat(List<DeckEntry> entries, List<String> violations) {
        List<DeckEntry> mainboard = entries.stream()
                .filter(e -> !e.isSideboard())
                .toList();
        int totalCards = mainboard.stream().mapToInt(DeckEntry::getQuantity).sum();
        if (totalCards < 40) {
            violations.add("Limited format decks must have at least 40 cards; found " + totalCards + ".");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String getLegalityForFormat(String legalitiesJson, String format) {
        if (legalitiesJson == null || legalitiesJson.isBlank()) return "unknown";
        try {
            Map<String, String> legalities = objectMapper.readValue(legalitiesJson,
                    new TypeReference<Map<String, String>>() {});
            return legalities.getOrDefault(format, "unknown");
        } catch (Exception e) {
            log.warn("Failed to parse legalitiesJson: {}", legalitiesJson, e);
            return "unknown";
        }
    }

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
