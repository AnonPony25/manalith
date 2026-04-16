package dev.manalith.catalog.repository;

import dev.manalith.catalog.model.CardPrinting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CardPrinting}.
 */
@Repository
public interface CardPrintingRepository extends JpaRepository<CardPrinting, UUID> {

    /**
     * Case-insensitive substring search on the card name.
     * Backed by the {@code idx_card_printings_name} GIN trigram index in PostgreSQL.
     */
    Page<CardPrinting> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Return all printings for a given set (expansion code, e.g. {@code "MOM"}).
     */
    Page<CardPrinting> findBySetCode(String setCode, Pageable pageable);

    /**
     * Return all printings that share a given Oracle ID.
     * Useful for showing reprint history.
     */
    Page<CardPrinting> findByOracleId(UUID oracleId, Pageable pageable);

    /**
     * Find the first printing with an exact name match.
     * Used by the named-card lookup endpoint.
     */
    Optional<CardPrinting> findFirstByName(String name);

    /**
     * Find the first printing whose name matches the given pattern (LIKE/ILIKE).
     * Used for fuzzy name lookups — pass a {@code %…%} wildcard string.
     */
    @Query("SELECT c FROM CardPrinting c WHERE LOWER(c.name) LIKE LOWER(:pattern)")
    Optional<CardPrinting> findFirstByNameLikeIgnoreCase(@Param("pattern") String pattern);

    /**
     * Colour-identity subset search.
     *
     * <p><strong>TODO — native query required.</strong>
     * A pure JPQL implementation is not feasible because filtering cards whose
     * {@code color_identity} is a <em>subset</em> of a given set of colours
     * requires PostgreSQL array containment operators ({@code @>} / {@code <@}).
     * Replace this stub with a {@code @Query(nativeQuery = true)} using:
     * <pre>{@code
     *   SELECT * FROM card_printings
     *   WHERE color_identity <@ CAST(:colors AS VARCHAR(8)[])
     * }</pre>
     * where {@code :colors} is bound as a {@code String[]} parameter.
     */
    @Query("SELECT c FROM CardPrinting c WHERE c.id IS NOT NULL")
    Page<CardPrinting> findByColorIdentitySubset(@Param("colors") String colorsJson, Pageable pageable);

    /**
     * Check whether a printing exists and was updated more recently than {@code cutoff}.
     * Used by the sync job to skip re-importing cards that are already up-to-date.
     */
    boolean existsByIdAndUpdatedAtAfter(UUID id, Instant cutoff);
}
