package dev.manalith.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * DTO returned to API consumers for a single card printing.
 *
 * <p>Fields mirror the Scryfall card object structure. Nested records handle
 * image URIs, card faces (for double-faced/split cards), and price data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardPrintingDTO(
        String id,
        String oracleId,
        String name,
        String setCode,
        String setName,
        String collectorNumber,
        String rarity,
        String layout,
        String manaCost,
        double cmc,
        String typeLine,
        String oracleText,
        List<String> colors,
        List<String> colorIdentity,
        Map<String, String> legalities,
        ImageUrisDTO imageUris,
        List<CardFaceDTO> cardFaces,
        CardPricesDTO prices,
        String releasedAt
) {

    /**
     * Image URI set for a card or card face.
     *
     * @param small      57 × 80 px thumbnail
     * @param normal     488 × 680 px standard display size
     * @param large      672 × 936 px high-resolution
     * @param png        745 × 1040 lossless PNG
     * @param artCrop    art crop (variable aspect ratio)
     * @param borderCrop full card with border crop (480 × 680 px)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImageUrisDTO(
            String small,
            String normal,
            String large,
            String png,
            String artCrop,
            String borderCrop
    ) {}

    /**
     * A single face of a multi-faced card (double-faced, split, flip, etc.).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CardFaceDTO(
            String name,
            String manaCost,
            String typeLine,
            String oracleText,
            ImageUrisDTO imageUris
    ) {}

    /**
     * Current market prices for a printing.
     *
     * @param usd      non-foil USD price (may be {@code null} if unavailable)
     * @param usdFoil  foil USD price (may be {@code null})
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CardPricesDTO(
            String usd,
            String usdFoil
    ) {}
}
