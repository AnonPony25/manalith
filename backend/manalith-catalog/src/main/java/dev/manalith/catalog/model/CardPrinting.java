package dev.manalith.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code card_printings} table.
 *
 * <p>JSONB columns ({@code legalities}, {@code image_uris}, {@code card_faces},
 * {@code prices}) are persisted as raw JSON strings. The service layer is
 * responsible for deserialising them via Jackson.
 *
 * <p>The {@code colors} and {@code color_identity} PostgreSQL array columns are
 * stored as JSON strings here too; a custom Hibernate type would be needed for
 * native array mapping, so we keep it simple and let the service parse them.
 */
@Entity
@Table(name = "card_printings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPrinting {

    /** Scryfall UUID — not auto-generated; assigned by Scryfall. */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "oracle_id", nullable = false)
    private UUID oracleId;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "set_code", nullable = false, length = 8)
    private String setCode;

    @Column(name = "set_name", length = 255)
    private String setName;

    @Column(name = "collector_number", length = 16)
    private String collectorNumber;

    @Column(name = "rarity", length = 16)
    private String rarity;

    @Column(name = "layout", length = 32)
    private String layout;

    @Column(name = "mana_cost", length = 128)
    private String manaCost;

    @Column(name = "cmc", precision = 6, scale = 2)
    private BigDecimal cmc;

    @Column(name = "type_line", length = 512)
    private String typeLine;

    @Column(name = "oracle_text", columnDefinition = "TEXT")
    private String oracleText;

    /**
     * Serialised JSON array of colour symbols (e.g. {@code ["W","U"]}).
     * Stored as TEXT rather than the native {@code VARCHAR(8)[]} array for
     * portability; the service layer deserialises this field.
     */
    @Column(name = "colors", columnDefinition = "TEXT")
    private String colorsJson;

    /**
     * Serialised JSON array of colour-identity symbols.
     */
    @Column(name = "color_identity", columnDefinition = "TEXT")
    private String colorIdentityJson;

    /** JSONB column — format legalities map, e.g. {@code {"standard":"legal",...}}. */
    @Column(name = "legalities", columnDefinition = "jsonb")
    private String legalitiesJson;

    /** JSONB column — Scryfall image URI map. */
    @Column(name = "image_uris", columnDefinition = "jsonb")
    private String imageUrisJson;

    /** JSONB column — array of card faces (for split/flip/double-faced cards). */
    @Column(name = "card_faces", columnDefinition = "jsonb")
    private String cardFacesJson;

    /** JSONB column — current price data (USD, USD foil, EUR, …). */
    @Column(name = "prices", columnDefinition = "jsonb")
    private String pricesJson;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
