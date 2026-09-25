# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository shape

Elingo is a language-learning system split into three packages that mirror each other's module layout:

- `backend/` — Spring Boot 4.1.1 (Java 21, Maven) REST + WebSocket API.
- `frontend/` — React 19 + Vite 8, JavaScript.
- `ai-service/` — Python microservice for AI-assisted pronunciation scoring.

The three are wired by convention: every backend module (`learning/flashcard`, `vocabulary`, `battle`, ...) has a matching frontend module under `frontend/src/modules/`. Keep that correspondence when adding features.

> **Current state:** the codebase is a freshly restructured skeleton. The backend contains only `ElingoApplication.java` plus empty `package-info.java` placeholders; `ai-service/` files are empty stubs; `frontend/` has directory scaffolding and empty service/context files. **The authoritative design lives in `docs/`, not in code** — read it before implementing a module so you build what was designed rather than guessing.

## Authoritative documentation (read these first)

- `docs/project-architecture/{backend,frontend,ai-service}-architecture.md` — intended package structure, layering, and responsibilities for each of the three services.
- `docs/database-schema.md` — the MySQL schema (tables, columns, FKs). Source of truth for entity fields.
- `docs/diagrams/erd/*.puml` — PlantUML ERDs grouped by domain (vocabulary, learning, battle, gamification, community, payment, speaking-notification).
- `docs/diagrams/sequence-diagram/*.puml` — flows for auth/profile/password/email and dictation.
- `.claude/rules/naming-conventions.md` — **binding naming rules** for all three languages (loaded as project instructions). Follow them exactly: e.g. backend packages are lowercase (`speaking`, not `speakingRoom`), DTOs are split `request/`+`response/` with `Request`/`Response` suffixes (never a bare `AuthDTO`), REST endpoints are kebab-case plural nouns, Python is `snake_case`, React components are `PascalCase`.

## Frontend Foundation Documents (read before any FE work)

Four markdown files in `frontend/docs/` define the official standards — **all AI assistants and team members must read them before writing any frontend code**:

- `frontend/docs/FE_CONVENTIONS.md` — Stack, folder structure, naming conventions, routing, API patterns, state management, component patterns, form validation, comment style, and a full list of rules (must-do and must-not).
- `frontend/docs/FE_DESIGN_SYSTEM.md` — Visual system ("Liquid Glass Minimalism"): color tokens, typography scale, spacing, border-radius, glass CSS classes, button system, cards, chips/badges, inputs, icons, transitions, and a per-screen checklist.
- `frontend/docs/FE_ROUTES.md` — Full route map (public / private user / admin), dynamic route regex patterns, route guard logic, query param conventions, and rules for adding new routes.
- `frontend/docs/FE_COMPONENT_GUIDELINES.md` — When to create shared vs module-specific components, component file structure, template, props conventions, how to use the design system inside components, API reference for all current shared components, and common mistakes to avoid.

## Commands

### Backend (run from `backend/`)
```bash
./mvnw spring-boot:run                      # run the API (Spring Boot default port 8080)
./mvnw test                                 # all tests
./mvnw test -Dtest=ElingoApplicationTests   # single test class
./mvnw test -Dtest=ClassName#methodName     # single test method
./mvnw clean package                        # build jar
```
Uses Lombok (annotation processor is configured in `pom.xml`) and `spring-boot-devtools` for hot reload.

### Frontend (run from `frontend/`)
```bash
npm install
npm run dev        # Vite dev server
npm run build      # production build
npm run lint       # oxlint
npm run preview    # serve the production build
```
Linting is **oxlint** (`.oxlintrc.json`), not ESLint. No test runner is wired up yet — `src/tests/` and `src/setupTests.js` exist but `package.json` has no test script; add Vitest before writing tests.

### AI service (run from `ai-service/`)
`requirements.txt` is currently empty and the modules are stubs. Per the architecture doc this is a **FastAPI** service intended to be run on its own port; once populated, install deps with `pip install -r requirements.txt` and launch via uvicorn against `api/routes.py`.

## Architecture

### Backend — layered modules + event-driven gamification
Each module under `com.elingo.<module>` follows the same layers: `controller/` (validate + delegate, no business logic) → `service/` (interface + `Impl`, all logic and `@Transactional`) → `repository/` (`JpaRepository`) → `entity/` (`@Entity`, extends a common `BaseEntity`) → `dto/` (split `request/`/`response/`; never return entities directly). `common/` holds cross-cutting concerns (`dto/ApiResponse`, `exception/GlobalExceptionHandler`, `util/`, `event/`).

Two design rules worth preserving:
- **`gamification/` is purely event-driven.** Learning modules (`flashcard`, `dictation`, `shadowing`, `lesson`, `battle`) publish events to `common/event/` (e.g. `FlashcardReviewedEvent`, `LessonCompletedEvent`, `BattleWonEvent`); `gamification/` listeners consume them to award XP/streaks/levels. This keeps gamification from depending back on learning modules — adding a new XP source means a new event + listener, not edits to the learning module.
- **`progress/` (statistics) is separate from `gamification/` (rewards).** `progress/` only *reads* from other modules' repositories to build dashboards and owns no business entities; XP/level/streak live exclusively in `gamification/`.

`learning/` is one package with four independent sub-packages (`flashcard`, `lesson`, `dictation`, `shadowing`) because each has different scoring logic but shares lesson/deck data. `battle/` and `speaking/` are realtime (WebSocket/STOMP for battle, WebRTC signaling for speaking rooms).

### Frontend — feature modules mirroring the backend
`src/modules/<feature>/` each contains `components/`, `pages/`, and a module-local API file. Shared, business-agnostic UI lives in `src/components/` (PascalCase dirs), global state in `src/context/` (`AuthContext`, `ThemeContext`), and the Axios instance in `src/services/apiClient.js` (attaches the JWT, centralizes error handling). The API base URL comes from `VITE_API_URL` (`src/.env.example`), with `VITE_`-prefixed env vars per the naming rules.

### AI service — two-tier pronunciation scoring
A standalone microservice the backend calls over REST. Spring Boot stores the user's audio (S3 or shared dir) and sends a URL + transcript rather than streaming bytes. The pipeline is two tiers:
- **Tier 1 (`modules/tier1_extraction/`)** — feature extraction: `prosody.py` (F0/energy via parselmouth), `lexicon.py` (lexicon constraint), `alignment.py` (Montreal Forced Aligner → phoneme timestamps + probabilities via Wav2Vec2/HuBERT).
- **Tier 2 (`modules/tier2_scoring/`)** — scoring: `gop.py` (Goodness of Pronunciation), `intonation.py` (DTW comparison of F0 curves against native audio), `rhythm.py` (speech rate + pause metrics from alignment timestamps).

`core/orchestrator.py` runs Tier 1 → Tier 2; `api/routes.py` is the HTTP boundary. The request/response payload contract is documented in `docs/project-architecture/ai-service-architecture.md`.

## Data & storage
- Primary database is **MySQL** (schema in `docs/database-schema.md`): `BIGINT UNSIGNED AUTO_INCREMENT` PKs, `created_at`/`updated_at` timestamps, `utf8mb4`, InnoDB.
- Audio for AI scoring is intended to be object storage (S3/Cloudinary); realtime features use WebSocket/STOMP and WebRTC.
