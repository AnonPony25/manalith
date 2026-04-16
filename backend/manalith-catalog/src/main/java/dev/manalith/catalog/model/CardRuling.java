package dev.manalith.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for the {@code card_rulings} table.
 *
 * <p>Rulings are linked to a card via {@code oracle_id} rather than
 * the printing ID because a ruling applies to all printings that share
 * the same Oracle text.
 */
@Entity
@Table(name = "card_rulings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRuling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "oracle_id", nullable = false)
    private UUID oracleId;

    /** Either {@code "wotc"} or {@code "scryfall"}. */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
