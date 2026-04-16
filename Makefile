# Manalith — Developer shortcuts

.PHONY: infra infra-down backend frontend forge-build test-backend test-frontend

infra:
	docker compose up -d postgres redis

infra-down:
	docker compose down

forge-build:
	scripts/build-forge.sh

backend:
	cd backend && mvn spring-boot:run -pl manalith-api -Pdev

frontend:
	cd frontend && npm run dev

test-backend:
	cd backend && mvn test

test-frontend:
	cd frontend && npm test

build-all:
	cd backend && mvn clean install -DskipTests
	cd frontend && npm run build

lint:
	cd frontend && npm run lint
	cd backend && mvn checkstyle:check
