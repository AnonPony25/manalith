# Local Development Guide

## Prerequisites

| Tool | Minimum version | Notes |
|---|---|---|
| Java JDK | 17 | Eclipse Temurin recommended |
| Maven | 3.9 | or use `./mvnw` wrapper |
| Node.js | 20 LTS | |
| npm | 10+ | |
| Docker Desktop | Latest | For Postgres + Redis |
| Git | 2.40+ | |

## 1. Clone and configure

```bash
git clone https://github.com/manalith/manalith.git
cd manalith
cp .env.example .env
# Edit .env — minimum: set JWT_SECRET to any 32+ char string
```

## 2. Build Forge engine locally

Forge must be built from source and published to your local Maven repository
before the backend will compile.

```bash
# Option A — use the helper script (Windows)
scripts\build-forge.bat

# Option B — manual
git clone https://github.com/Card-Forge/forge.git ../forge
cd ../forge
mvn clean install -pl forge-core,forge-game,forge-ai -am -DskipTests
```

This publishes `forge-core`, `forge-game`, `forge-ai` with version
`LOCAL-SNAPSHOT` to `~/.m2/repository/org/cardforge/`.

## 3. Start infrastructure

```bash
docker compose up -d
# Postgres is on localhost:5432
# Redis is on localhost:6379
```

Flyway will run migrations automatically when the backend starts.

## 4. Run the backend

From VS Code: use the **Spring Boot Dashboard** or run the
`Manalith Backend (Spring Boot)` launch config.

From terminal:
```bash
cd backend
mvn spring-boot:run -pl manalith-api -Pdev
```

The API will be available at `http://localhost:8080`.

## 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
# Opens at http://localhost:5173
# Proxies /api and /ws to localhost:8080 (see vite.config.ts)
```

## 6. Running tests

```bash
# Backend (unit + integration with Testcontainers)
cd backend && mvn test

# Frontend
cd frontend && npm test
```

## Module Dependency Map

```
manalith-api
  └── manalith-auth
  └── manalith-catalog
  └── manalith-deck
      └── manalith-catalog
  └── manalith-collection
      └── manalith-catalog
  └── manalith-lobby
  └── manalith-game
      └── manalith-forge-adapter
          └── forge-core (external)
          └── forge-game (external)
          └── forge-ai   (external, optional)
      └── manalith-lobby
      └── manalith-catalog
  └── manalith-match-history
  └── manalith-notifications
  └── manalith-common  ← all modules depend on this
```

## Scryfall Bulk Sync

On first run, trigger a manual sync via:

```bash
curl -X POST http://localhost:8080/api/catalog/sync
```

Or wait for the nightly cron (4 AM by default). This populates
the `card_printings` and `card_rulings` tables from Scryfall bulk data.
Expect ~300 MB download and ~2 minutes to upsert ~27,000 cards.
