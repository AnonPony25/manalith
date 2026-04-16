# ADR-001: Modular Monolith over Microservices

**Status:** Accepted  
**Date:** 2026-04-15

## Context

The platform needs to be self-hostable on a single VPS by a community admin,
and maintainable by a 2–5 person open-source team. The stateful Forge game
engine is the hardest component.

## Decision

Use a **modular monolith**: a single Spring Boot application with clearly
separated Java packages deployed as one JAR alongside PostgreSQL and Redis.

## Consequences

- Simpler deployment and contributor onboarding vs. microservices.
- Internal module boundaries enforced via Maven multi-module structure.
- Future extraction remains possible if scale requires.
