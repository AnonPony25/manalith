package dev.manalith.catalog.repository;

import dev.manalith.catalog.model.CardRuling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CardRuling}.
 */
@Repository
public interface CardRulingRepository extends JpaRepository<CardRuling, Long> {

    /**
     * Return all rulings for the given Oracle ID, ordered by publication date.
     */
    List<CardRuling> findByOracleId(UUID oracleId);

    /**
     * Delete all rulings for the given Oracle ID.
     * Called during bulk sync to replace stale rulings with the latest data.
     */
    @Modifying
    @Transactional
    void deleteByOracleId(UUID oracleId);
}
