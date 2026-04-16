# Manalith Development Roadmap

## Phase 0 — Project Setup ✅
- [x] Git monorepo (backend, frontend, infra)
- [x] Spring Boot 3 + Java 17 Maven multi-module
- [x] React 18 + TypeScript + Vite frontend
- [x] Docker Compose (Postgres + Redis)
- [x] Dockerfile (backend + frontend)
- [x] Nginx reverse proxy config
- [x] VS Code workspace + launch configs
- [x] GitHub Actions CI
- [x] Checkstyle + ESLint + Prettier
- [x] .editorconfig
- [x] README + CONTRIBUTING + PROTOCOL docs

## Phase 1 — Forge Rules Engine Integration 🚧
- [x] ForgeGameAdapter service
- [x] ForgeGameSession / ForgePlayerSeat / ForgeGameConfig models
- [x] NetworkPlayerController (blocking inbox pattern)
- [x] GameStateSerializer stub
- [x] GameSessionService
- [x] WebSocketGameEventPublisher
- [ ] Forge submodule / build integration (requires local Forge build)
- [ ] AI vs AI headless test
- [ ] JUnit integration tests with Forge on classpath

## Phase 2 — Scryfall Card Catalog 🚧
- [x] CardPrinting + CardRuling JPA entities
- [x] CardPrintingRepository + CardRulingRepository
- [x] ScryfallApiClient (bulk data metadata + download)
- [x] ScryfallBulkSyncJob (nightly cron + startup sync)
- [x] ScryfallCardImporter (streaming JSON parse, batch upsert)
- [x] CardCatalogService (search, getById, getByName)
- [x] CardCatalogController (REST API)
- [ ] Image proxy/cache endpoint
- [ ] Full Scryfall search syntax support
- [ ] Sets endpoint

## Phase 3 — Authentication 🚧
- [x] User / OAuthIdentity / RefreshToken entities
- [x] JwtService (generate, validate, extract)
- [x] AuthService (OAuth user creation, token pair issuance, rotation)
- [x] SecurityConfig (Spring Security 6)
- [x] JwtAuthenticationFilter
- [x] OAuth2AuthenticationSuccessHandler
- [x] AuthController (refresh, logout, /me)
- [x] Frontend authStore (Zustand)
- [x] Frontend authApi (Axios + interceptors)
- [x] LoginPage + AuthCallbackPage
- [ ] Rate limiting on auth endpoints
- [ ] Email verification (optional)

## Phase 4 — Deck Builder
- [ ] Deck CRUD REST API
- [ ] Deck legality validation
- [ ] Import: plain text, MTGO .dec, Arena
- [ ] Export: all formats
- [ ] Frontend deck builder UI
- [ ] Mana curve + color distribution charts

## Phase 5 — Collection Manager
- [ ] user_card_holdings CRUD
- [ ] Import: MTGO, Moxfield, TCGPlayer CSV
- [ ] Nightly price updates
- [ ] Frontend collection grid UI

## Phase 6 — Lobby & Matchmaking
- [ ] Lobby CRUD (create, join, leave)
- [ ] Invite codes
- [ ] Casual queue per format
- [ ] Frontend lobby UI
- [ ] Lobby → Game handoff

## Phase 7 — 1v1 Game Client
- [ ] Full NetworkPlayerController Forge integration
- [ ] GameStateView serialization
- [ ] Frontend PixiJS game board
- [ ] Priority / stack / phase display
- [ ] Delta updates

## Phase 8 — Commander / Multiplayer
- [ ] 2–4 player support
- [ ] Commander zone + damage
- [ ] Spectator mode

## Phase 9 — Polish & QA
## Phase 10 — Deployment & Operations
