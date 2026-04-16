package dev.manalith.deck.repository;

import dev.manalith.deck.model.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<Deck> findByOwnerIdAndFormatOrderByUpdatedAtDesc(UUID ownerId, String format);

    Page<Deck> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);

    Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    long countByOwnerId(UUID ownerId);
}
