# Contributing to Manalith

Thank you for your interest in contributing. Manalith is GPL v3 licensed.
All contributions must be compatible with GPL v3.

## Development Setup

See [docs/DEVELOPMENT.md](./docs/DEVELOPMENT.md) for full local setup.

**Quick start:**
```bash
# 1. Clone
git clone https://github.com/manalith/manalith.git
cd manalith

# 2. Start infra (Postgres + Redis)
docker compose up -d

# 3. Copy and fill in env
cp .env.example .env

# 4. Run backend (Java 17+ required)
cd backend && mvn spring-boot:run -pl manalith-api -Pdev

# 5. Run frontend (Node 20+ required)
cd ../frontend && npm install && npm run dev
```

## Branch Model

| Branch | Purpose |
|---|---|
| `main` | Stable, tagged releases |
| `develop` | Integration branch for feature PRs |
| `feature/<name>` | Individual features |
| `fix/<name>` | Bug fixes |
| `docs/<name>` | Documentation only changes |

## Pull Request Checklist

- [ ] Branch from `develop`, not `main`
- [ ] `mvn test` passes for all backend modules
- [ ] `npm run lint` and `npm test` pass for frontend
- [ ] New behavior is covered by unit or integration tests
- [ ] PR description explains **what** and **why** (not just what changed)
- [ ] Forge-adapter changes include notes on which `PlayerController` methods are affected

## Code Style

- **Java:** Google Java Style Guide. Configured via Checkstyle (coming Phase 0).
- **TypeScript:** ESLint + Prettier (`.prettierrc` in `frontend/`).
- **SQL:** Lowercase keywords, snake_case identifiers.

## Reporting Bugs

Open a GitHub Issue with:
1. Steps to reproduce
2. Expected vs actual behavior
3. Java version, OS, browser

## Legal Notes

- By submitting a PR you agree to license your contribution under GPL v3.
- Do not include any Wizards of the Coast proprietary assets (card images are served from Scryfall CDN).
- Forge card scripts are part of the Forge project and covered by its GPL v3 license.
