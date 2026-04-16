package dev.manalith.deck.model;

import dev.manalith.catalog.model.CardPrinting;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deck_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CardPrinting card;

    @Builder.Default
    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "is_commander")
    private boolean isCommander;

    @Column(name = "is_sideboard")
    private boolean isSideboard;
}
