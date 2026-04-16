# Manalith

**A free, open-source, real-time multiplayer Magic: The Gathering platform.**

Manalith uses [Forge](https://github.com/Card-Forge/forge) as its authoritative, embedded rules engine and [Scryfall](https://scryfall.com/docs/api) as the single source of truth for card data and imagery. It supports 1v1 and Commander (2–4 player) games over a browser-based client with a WebSocket game protocol.

---

## Architecture at a Glance

```
manalith/
├── backend/          Java 17 + Spring Boot 3 (modular monolith)
│   ├── manalith-common          Shared DTOs, exceptions, utilities
│   ├── manalith-forge-adapter   Forge engine headless integration layer
│   ├── manalith-api             HTTP REST controllers, WebSocket endpoints
│   ├── manalith-auth            JWT + OAuth2 (Discord / GitHub)
│   ├── manalith-catalog         Scryfall bulk sync, card search, legality
│   ├── manalith-deck            Deck CRUD, import/export, validation
│   ├── manalith-collection      User card holdings
│   ├── manalith-lobby           Room creation, matchmaking, invites
│   ├── manalith-game            Game session lifecycle, action intake
│   ├── manalith-match-history   Results, replays, post-game summaries
│   └── manalith-notifications   In-app alerts and social events
├── frontend/         React 18 + TypeScript + PixiJS
├── infra/            Docker Compose, Nginx, Postgres, Redis config
├── docs/             Architecture decisions, protocol spec, contributing guides
└── scripts/          Dev helper scripts (db seed, Forge build, etc.)
```

## Technology Stack

| Layer | Choice |
|---|---|
| Backend language | Java 17+ |
| Backend framework | Spring Boot 3.x |
| Rules engine | Forge (`forge-core`, `forge-game`, `forge-ai`) |
| Real-time transport | WebSocket + JSON event protocol |
| Primary database | PostgreSQL 16 |
| Cache / broker | Redis 7 |
| Card data | Scryfall Bulk Data API |
| Frontend framework | React 18 + TypeScript |
| Game board rendering | PixiJS |
| Frontend state | Zustand (game) + TanStack Query (REST) |
| Auth | JWT + OAuth2 (Discord, GitHub) |
| Build tool (backend) | Maven |
| Build tool (frontend) | Vite |
| Containerization | Docker + Docker Compose |

## License

GPL v3 — see [LICENSE](./LICENSE). Forge is also GPL v3, and Manalith links against it as a library. All derivative works must remain GPL v3 compatible.

## Getting Started

See [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md) for local setup instructions.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md).
