package dev.manalith.deck.repository;

import dev.manalith.deck.model.DeckEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckEntryRepository extends JpaRepository<DeckEntry, Long> {

    List<DeckEntry> findByDeckId(UUID deckId);

    @Modifying
    @Transactional
    void deleteByDeckId(UUID deckId);

    Optional<DeckEntry> findByDeckIdAndCardIdAndIsSideboard(UUID deckId, UUID cardId, boolean isSideboard);
}
