package dev.manalith.deck.dto;

import java.util.List;

public record LegalityResultDTO(
        boolean isLegal,
        String format,
        List<String> violations
) {
    public static LegalityResultDTO legal(String format) {
        return new LegalityResultDTO(true, format, List.of());
    }

    public static LegalityResultDTO illegal(String format, List<String> violations) {
        return new LegalityResultDTO(false, format, List.copyOf(violations));
    }
}
