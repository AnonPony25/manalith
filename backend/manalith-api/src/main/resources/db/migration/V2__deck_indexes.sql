-- ============================================================
-- V2 — Deck builder indexes and constraints
-- Adds composite indexes for common deck query patterns
-- ============================================================

-- Deck lookup by owner + format
CREATE INDEX IF NOT EXISTS idx_decks_owner_id
    ON decks(owner_id);

CREATE INDEX IF NOT EXISTS idx_decks_owner_format
    ON decks(owner_id, format);

-- Public deck browsing (sorted by recency)
CREATE INDEX IF NOT EXISTS idx_decks_public_updated
    ON decks(is_public, updated_at DESC)
    WHERE is_public = TRUE;

-- Deck entry card lookups
CREATE INDEX IF NOT EXISTS idx_deck_entries_card_id
    ON deck_entries(card_id);

-- Composite index for the "find entry by deck + card + sideboard" query
CREATE INDEX IF NOT EXISTS idx_deck_entries_deck_card_side
    ON deck_entries(deck_id, card_id, is_sideboard);

-- Commander entries
CREATE INDEX IF NOT EXISTS idx_deck_entries_commander
    ON deck_entries(deck_id, is_commander)
    WHERE is_commander = TRUE;
