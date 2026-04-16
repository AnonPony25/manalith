-- ============================================================
-- V1 — Initial Manalith Schema
-- ============================================================

-- Users
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(64) NOT NULL,
    email        VARCHAR(255) UNIQUE,
    avatar_url   VARCHAR(512),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- OAuth identities (one per provider per user)
CREATE TABLE oauth_identities (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider    VARCHAR(32) NOT NULL,     -- 'discord', 'github'
    provider_id VARCHAR(255) NOT NULL,
    UNIQUE (provider, provider_id)
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Card printings (populated by Scryfall bulk sync)
CREATE TABLE card_printings (
    id               UUID PRIMARY KEY,    -- Scryfall UUID
    oracle_id        UUID NOT NULL,
    name             VARCHAR(512) NOT NULL,
    set_code         VARCHAR(8) NOT NULL,
    set_name         VARCHAR(255),
    collector_number VARCHAR(16),
    rarity           VARCHAR(16),
    layout           VARCHAR(32),
    mana_cost        VARCHAR(128),
    cmc              NUMERIC(6,2),
    type_line        VARCHAR(512),
    oracle_text      TEXT,
    colors           VARCHAR(8)[],
    color_identity   VARCHAR(8)[],
    legalities       JSONB,
    image_uris       JSONB,
    card_faces       JSONB,
    prices           JSONB,
    released_at      DATE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_printings_oracle_id ON card_printings(oracle_id);
CREATE INDEX idx_card_printings_name ON card_printings USING gin(name gin_trgm_ops);
CREATE INDEX idx_card_printings_set_code ON card_printings(set_code);

-- Rulings
CREATE TABLE card_rulings (
    id         BIGSERIAL PRIMARY KEY,
    oracle_id  UUID NOT NULL,
    source     VARCHAR(32),
    published_at DATE,
    comment    TEXT
);

CREATE INDEX idx_card_rulings_oracle_id ON card_rulings(oracle_id);

-- Decks
CREATE TABLE decks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    format      VARCHAR(32) NOT NULL,
    description TEXT,
    is_public   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Deck entries
CREATE TABLE deck_entries (
    id           BIGSERIAL PRIMARY KEY,
    deck_id      UUID NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
    card_id      UUID NOT NULL REFERENCES card_printings(id),
    quantity     SMALLINT NOT NULL DEFAULT 1,
    is_commander BOOLEAN NOT NULL DEFAULT FALSE,
    is_sideboard BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_deck_entries_deck_id ON deck_entries(deck_id);

-- User collections
CREATE TABLE user_card_holdings (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id          UUID NOT NULL REFERENCES card_printings(id),
    quantity_nonfoil SMALLINT NOT NULL DEFAULT 0,
    quantity_foil    SMALLINT NOT NULL DEFAULT 0,
    acquisition_price NUMERIC(8,2),
    last_updated     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, card_id)
);

-- Lobbies
CREATE TABLE lobbies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128),
    format      VARCHAR(32) NOT NULL,
    max_players SMALLINT NOT NULL DEFAULT 2,
    is_private  BOOLEAN NOT NULL DEFAULT FALSE,
    status      VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Lobby seats
CREATE TABLE lobby_seats (
    id       BIGSERIAL PRIMARY KEY,
    lobby_id UUID NOT NULL REFERENCES lobbies(id) ON DELETE CASCADE,
    user_id  UUID REFERENCES users(id),
    deck_id  UUID REFERENCES decks(id),
    seat_index SMALLINT NOT NULL,
    is_ready BOOLEAN NOT NULL DEFAULT FALSE
);

-- Match history
CREATE TABLE matches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lobby_id    UUID REFERENCES lobbies(id),
    format      VARCHAR(32) NOT NULL,
    started_at  TIMESTAMPTZ NOT NULL,
    ended_at    TIMESTAMPTZ,
    winner_id   UUID REFERENCES users(id)
);

CREATE TABLE match_participants (
    id           BIGSERIAL PRIMARY KEY,
    match_id     UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id),
    seat_index   SMALLINT NOT NULL,
    final_life   SMALLINT,
    is_winner    BOOLEAN NOT NULL DEFAULT FALSE
);

-- Enable pg_trgm for fuzzy name search
CREATE EXTENSION IF NOT EXISTS pg_trgm;
