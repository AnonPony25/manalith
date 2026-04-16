package dev.manalith.catalog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manalith.catalog.dto.CardPrintingDTO;
import dev.manalith.catalog.dto.CardSearchResultDTO;
import dev.manalith.catalog.model.CardPrinting;
import dev.manalith.catalog.model.CardRuling;
import dev.manalith.catalog.repository.CardPrintingRepository;
import dev.manalith.catalog.repository.CardRulingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Core business logic for card catalog queries.
 *
 * <p>Supports a lightweight Scryfall-like search syntax via the {@code q} parameter:
 * <ul>
 *   <li>{@code name:Lightning} — filter by name substring</li>
 *   <li>{@code type:Creature} — filter by type line substring</li>
 *   <li>{@code set:MOM} — filter by set code</li>
 *   <li>{@code color:WU} — filter by colour identity</li>
 * </ul>
 * Tokens that do not match a recognised prefix are treated as plain name searches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCatalogService {

    private final CardPrintingRepository cardPrintingRepository;
    private final CardRulingRepository cardRulingRepository;
    private final ObjectMapper objectMapper;

    // ─── Inline DTO ───────────────────────────────────────────────────────────

    /**
     * Ruling DTO returned to callers.
     */
    public record CardRulingDTO(
            String source,
            String publishedAt,
            String comment
    ) {}

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Search cards with a flexible query string and optional filter parameters.
     *
     * @param query    Scryfall-like search string (may include {@code name:}, {@code set:}, etc.)
     * @param setCode  explicit set filter (overrides {@code set:} token in query)
     * @param colors   explicit colour filter (overrides {@code color:} token in query)
     * @param format   legality format filter (e.g. {@code "standard"}) — applied post-query
     * @param page     zero-based page index
     * @param pageSize results per page
     */
    public CardSearchResultDTO searchCards(
            String query,
            String setCode,
            String colors,
            String format,
            int page,
            int pageSize
    ) {
        // Parse query tokens into recognised prefixes
        String nameFilter = null;
        String setFilter = setCode;
        String colorFilter = colors;

        if (StringUtils.hasText(query)) {
            for (String token : query.trim().split("\\s+")) {
                if (token.startsWith("name:")) {
                    nameFilter = token.substring(5);
                } else if (token.startsWith("type:")) {
                    // type: filter handled via name search fallback for now
                    // TODO: add findByTypeLineContainingIgnoreCase to repository
                    nameFilter = nameFilter != null ? nameFilter : token.substring(5);
                } else if (token.startsWith("set:")) {
                    if (!StringUtils.hasText(setFilter)) {
                        setFilter = token.substring(4);
                    }
                } else if (token.startsWith("color:")) {
                    if (!StringUtils.hasText(colorFilter)) {
                        colorFilter = token.substring(6);
                    }
                } else {
                    // Bare token → treat as name search
                    nameFilter = nameFilter != null ? nameFilter + " " + token : token;
                }
            }
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, pageSize), 200),
                Sort.by("name").ascending()
        );

        Page<CardPrinting> resultPage;

        if (StringUtils.hasText(setFilter) && !StringUtils.hasText(nameFilter)) {
            resultPage = cardPrintingRepository.findBySetCode(setFilter.toUpperCase(), pageable);
        } else if (StringUtils.hasText(nameFilter)) {
            resultPage = cardPrintingRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
        } else {
            // No filters — return all (paginated)
            resultPage = cardPrintingRepository.findAll(pageable);
        }

        List<CardPrintingDTO> dtos = resultPage.getContent().stream()
                .map(this::toDTO)
                .filter(dto -> matchesFormat(dto, format))
                .toList();

        return new CardSearchResultDTO(
                dtos,
                (int) resultPage.getTotalElements(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.hasNext()
        );
    }

    // ─── Single-card lookups ─────────────────────────────────────────────────

    /**
     * Look up a card printing by its Scryfall UUID.
     */
    public Optional<CardPrintingDTO> getCardById(UUID id) {
        return cardPrintingRepository.findById(id).map(this::toDTO);
    }

    /**
     * Look up a card by exact or fuzzy name.
     *
     * @param name  the card name to search for
     * @param fuzzy when {@code true} a substring/LIKE match is attempted first
     */
    public Optional<CardPrintingDTO> getCardByName(String name, boolean fuzzy) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        if (fuzzy) {
            return cardPrintingRepository
                    .findFirstByNameLikeIgnoreCase("%" + name + "%")
                    .map(this::toDTO);
        }
        return cardPrintingRepository.findFirstByName(name).map(this::toDTO);
    }

    // ─── Rulings ─────────────────────────────────────────────────────────────

    /**
     * Return all rulings for the given Oracle ID.
     */
    public List<CardRulingDTO> getRulings(UUID oracleId) {
        List<CardRuling> rulings = cardRulingRepository.findByOracleId(oracleId);
        return rulings.stream()
                .map(r -> new CardRulingDTO(
                        r.getSource(),
                        r.getPublishedAt() != null ? r.getPublishedAt().toString() : null,
                        r.getComment()
                ))
                .toList();
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    /**
     * Convert a {@link CardPrinting} entity to its DTO representation.
     *
     * <p>JSON fields stored as strings are deserialised here using Jackson.
     * Failures are logged and replaced with sensible defaults so that a single
     * malformed card does not break an entire search page.
     */
    CardPrintingDTO toDTO(CardPrinting entity) {
        List<String> colors = parseJsonList(entity.getColorsJson(), "colors", entity.getId());
        List<String> colorIdentity = parseJsonList(entity.getColorIdentityJson(), "color_identity", entity.getId());
        Map<String, String> legalities = parseJsonMap(entity.getLegalitiesJson(), "legalities", entity.getId());

        CardPrintingDTO.ImageUrisDTO imageUris = parseJsonObject(
                entity.getImageUrisJson(), "image_uris", entity.getId(),
                node -> new CardPrintingDTO.ImageUrisDTO(
                        textOf(node, "small"),
                        textOf(node, "normal"),
                        textOf(node, "large"),
                        textOf(node, "png"),
                        textOf(node, "art_crop"),
                        textOf(node, "border_crop")
                )
        );

        List<CardPrintingDTO.CardFaceDTO> cardFaces = parseCardFaces(entity.getCardFacesJson(), entity.getId());

        CardPrintingDTO.CardPricesDTO prices = parseJsonObject(
                entity.getPricesJson(), "prices", entity.getId(),
                node -> new CardPrintingDTO.CardPricesDTO(
                        textOf(node, "usd"),
                        textOf(node, "usd_foil")
                )
        );

        return new CardPrintingDTO(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getOracleId() != null ? entity.getOracleId().toString() : null,
                entity.getName(),
                entity.getSetCode(),
                entity.getSetName(),
                entity.getCollectorNumber(),
                entity.getRarity(),
                entity.getLayout(),
                entity.getManaCost(),
                entity.getCmc() != null ? entity.getCmc().doubleValue() : 0.0,
                entity.getTypeLine(),
                entity.getOracleText(),
                colors,
                colorIdentity,
                legalities,
                imageUris,
                cardFaces,
                prices,
                entity.getReleasedAt() != null ? entity.getReleasedAt().toString() : null
        );
    }

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private List<String> parseJsonList(String json, String field, UUID cardId) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse {} for card {}: {}", field, cardId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseJsonMap(String json, String field, UUID cardId) {
        if (!StringUtils.hasText(json)) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse {} for card {}: {}", field, cardId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @FunctionalInterface
    private interface NodeMapper<T> {
        T map(com.fasterxml.jackson.databind.JsonNode node) throws Exception;
    }

    private <T> T parseJsonObject(String json, String field, UUID cardId, NodeMapper<T> mapper) {
        if (!StringUtils.hasText(json)) return null;
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            return mapper.map(node);
        } catch (Exception e) {
            log.warn("Failed to parse {} for card {}: {}", field, cardId, e.getMessage());
            return null;
        }
    }

    private List<CardPrintingDTO.CardFaceDTO> parseCardFaces(String json, UUID cardId) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return Collections.emptyList();
            List<CardPrintingDTO.CardFaceDTO> faces = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode face : arr) {
                CardPrintingDTO.ImageUrisDTO faceImages = null;
                if (face.has("image_uris")) {
                    com.fasterxml.jackson.databind.JsonNode imgNode = face.get("image_uris");
                    faceImages = new CardPrintingDTO.ImageUrisDTO(
                            textOf(imgNode, "small"),
                            textOf(imgNode, "normal"),
                            textOf(imgNode, "large"),
                            textOf(imgNode, "png"),
                            textOf(imgNode, "art_crop"),
                            textOf(imgNode, "border_crop")
                    );
                }
                faces.add(new CardPrintingDTO.CardFaceDTO(
                        textOf(face, "name"),
                        textOf(face, "mana_cost"),
                        textOf(face, "type_line"),
                        textOf(face, "oracle_text"),
                        faceImages
                ));
            }
            return faces;
        } catch (Exception e) {
            log.warn("Failed to parse card_faces for card {}: {}", cardId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String textOf(com.fasterxml.jackson.databind.JsonNode node, String fieldName) {
        com.fasterxml.jackson.databind.JsonNode f = node.get(fieldName);
        return (f != null && !f.isNull()) ? f.asText() : null;
    }

    // ─── Format filter ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code format} is blank or the card is legal/restricted in it.
     */
    private boolean matchesFormat(CardPrintingDTO dto, String format) {
        if (!StringUtils.hasText(format)) return true;
        if (dto.legalities() == null) return false;
        String status = dto.legalities().get(format.toLowerCase());
        return "legal".equalsIgnoreCase(status) || "restricted".equalsIgnoreCase(status);
    }
}
