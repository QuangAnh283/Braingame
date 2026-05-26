# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack multiplayer quiz platform (similar to Quizizz/Kahoot) with real-time gameplay via Socket.IO. The application lives in this `doan/` directory.

**Stack:** Spring Boot 3.5.6 (Java 21) backend + React 19 + TypeScript + Vite frontend, PostgreSQL 17, Redis, MinIO, Socket.IO.

## Development Commands

All commands run from the `doan/` directory.

### Infrastructure (run first)

```powershell
./src/main/docker/docker-up.ps1
```
```bash
./src/main/docker/docker-up.sh
```

### Backend

```bash
./mvnw spring-boot:run -Pdev-no-frontend-build
./mvnw clean verify
./mvnw clean package -DskipTests
```

### Frontend

```bash
npm run dev
npm run build
npm run lint
```

### Docker (full stack)

```bash
docker compose -f src/main/docker/app.yml up -d
```

## Architecture

### Backend (`src/main/java/org/example/quizizz/`)

- **`common/`** - Cross-cutting: Redis cache managers (`cache/`), Spring configs (`config/`), enums (`constants/`), global exception handler (`exception/`), rate limiting and resource ownership (`security/`)
- **`controller/api/`** - REST endpoints for Auth, Exam, Answer, Game, Leaderboard, AI, Health
- **`controller/socketio/`** - Socket.IO event handlers for real-time room/game events
- **`service/`** - Business logic; `helper/` has game timer, score calculation, achievement logic
- **`model/`** - DTOs and entities organized by domain
- **`repository/`** - Spring Data JPA repositories
- **`mapper/`** - MapStruct entity<->DTO mapping
- **`security/`** - JWT provider and authentication filters

### Spring Profiles

| Profile | Use |
|---------|-----|
| `dev` | Local with H2 or local DB |
| `dev-no-frontend-build` | Local backend-only dev |
| `docker` | Full Docker Compose stack |
| `prod` | Production |

Config: `src/main/resources/config/application*.yml`

### Frontend (`src/main/webapp/src/`)

Feature-based React structure:
- **`features/admin/`** - Admin dashboard
- **`features/player/`** - Game room and gameplay
- **`features/teacher/`** - Exam creation and management
- **`components/`** - Shared UI components
- **`services/`** - API clients and Socket.IO client
- **`stores/`** - Zustand state management
- **`routers/`** - React Router v7 configuration

Vite proxies `/api/*` to backend port 8080 during development.

### Real-time Flow

Socket.IO via `netty-socketio` on the backend. Room lifecycle events published via Spring Events (`common/event/`). Redis cache (`common/cache/`) manages game sessions, user presence, and JWT blacklisting.

### Key Integrations

- **OpenAI** - AI question generation (`controller/api/` AI endpoint, `service/Implement/`)
- **MinIO** - File/image uploads (S3-compatible)
- **Spring Mail + Thymeleaf** - Email templates in `src/main/resources/templates/`
- **Swagger/OpenAPI** - API docs at `/swagger-ui.html`

## Testing

JUnit 5 + Testcontainers (real PostgreSQL container). Timezone: `Asia/Ho_Chi_Minh`.

```bash
./mvnw test
./mvnw test -Dtest=ClassName
./mvnw test -Dtest=ClassName#methodName
```

## Environment Setup

Copy `src/main/docker/.env.example` to `src/main/docker/.env` and fill in PostgreSQL, Redis, MinIO, JWT secret, OpenAI key, and mail credentials. File is gitignored.
