# Digital Farm Lab — A14 AgriConnect 360 Capstone

Simulated farm (digital twin) where learners run scenarios such as reducing irrigation by 20% and see the effect on yield, water use and cost — computed by a real FAO-56 water balance + Mitscherlich fertiliser response engine.

## Stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 3.3, Java 21, PostgreSQL 18, Flyway, JWT (HMAC-SHA256) |
| Frontend | React 19, Vite 6, TypeScript, PWA (installable, offline) |
| Mobile | Capacitor 6 → real Android APK |
| Deploy | Backend: Render · Database: Render Postgres · Local: Docker Compose |

## Live URLs

- **Backend API:** https://farmlab-backend-57i8.onrender.com
- **Swagger UI:** https://farmlab-backend-57i8.onrender.com/swagger-ui/index.html
- **Health check:** https://farmlab-backend-57i8.onrender.com/actuator/health

## Quick start (local)

    # Backend — via Docker (recommended)
    docker compose up -d --build
    docker compose ps              # all three: Up (healthy)

    # Frontend dev server
    cd digital-farm-lab-frontend
    npm install
    npm run dev                    # http://127.0.0.1:3000

Or run backend natively (needs local Postgres on 5432):

    cd digital-farm-lab-backend
    ./gradlew bootRun              # http://localhost:8081

## Features

1. **Farm Setup** — name, location, water sources, weather
2. **Farm Builder** — drag-drop fields, soil, crops, irrigation, water sources
3. **Scenario Management** — 12 categories, editable learning objectives
4. **Simulation Workspace** — animated simulator with 2D crop visuals per crop type
5. **Results & Learning** — KPIs, trend charts, scoring breakdown, exercise reflection
6. **Trainer** — cohort KPIs, score distribution, criteria satisfaction, recent submissions

## Real vs Mock Audit

### REAL

| Component | Proof |
|---|---|
| Spring Boot backend | Deployed to Render, healthcheck `UP` |
| PostgreSQL 18 | Render managed Postgres, migrations V1-V8 applied |
| FAO-56 scenario engine | `ScenarioEngine.java` — deterministic, 12 unit tests pass |
| JWT verification | HMAC-SHA256, 401/403 distinction, `@PreAuthorize` |
| Workspace API | `POST/GET /api/v1/workspace/{farms,scenarios,runs}` — persists to DB |
| Trainer report | SQL aggregation: AVG, median, pass rate, criterion satisfaction |
| Exercise submission | `POST /api/v1/exercises/{id}/submissions` — persists score + breakdown |
| Sensor readings | 90 readings × 3 types × N fields seeded by V8 migration |
| PWA | `manifest.webmanifest`, `sw.js`, installable, offline cache |
| Android APK | Capacitor 6 + real `.apk` file |

### MOCK

| Component | Mock file | Real system needed |
|---|---|---|
| AgriLearn catalogue | `MockAgriLearnClient.java` | A3 AgriLearn team's REST API |
| Skills Passport | `MockPassportClient.java` | A2 Skills Passport team's REST API |
| Identity issuer | Dev JWT (shared secret) | A1 MyAgriSETA OAuth2/OIDC |

Activate real integrations once partner teams deploy:

    # application-real.yml
    spring.profiles.active: real
    agriconnect.passport.base-url: https://a2-passport.up.railway.app
    agriconnect.learn.base-url:    https://a3-agrilearn.up.railway.app
    security.jwt.jwk-set-uri:      https://a1-myagriseta.up.railway.app/.well-known/jwks.json

### TEACHING LAYER (client-side, not scientific)

| Component | Purpose |
|---|---|
| Animated soil moisture / growth curve | Visual demonstration (client-side `Math.sin`) |
| Event log milestones | Teaching narrative |
| Animation counters (water used, growth %) | Visual pacing |

The **scores on Results** use the real FAO-56 output — the A14 ENGINE panel shows the raw values.

## A14 rubric coverage

| Requirement | Status |
|---|---|
| Virtual farm with fields, soil moisture, weather, crop model | Done — V8 sensor data + FAO-56 engine |
| Scenario runner (irrigation, fertiliser, planting date) | Done — `/api/v1/demo/run` + Engine modal |
| Results view (yield, water, cost vs baseline) | Done — Results page + A14 ENGINE panel |
| Scored exercises + reflection prompts | Done — ReflectionPanel on Results |
| Trainer view of cohort results | Done — Trainer page with KPIs, distribution, criteria |

## Testing

    # Backend unit tests (12)
    cd digital-farm-lab-backend
    ./gradlew test

    # Frontend build (TS + Vite + PWA)
    cd digital-farm-lab-frontend
    npm run build

    # Full Docker stack
    docker compose up -d --build

## Deployment

### Backend (Render)

1. Connect GitHub repo `digital-farm-lab-backend` to Render as a Web Service
2. Build: `./gradlew bootJar` → Run: `java -jar build/libs/*.jar`
3. Env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=docker`
4. Deploy → Flyway runs V1-V8 automatically

### Frontend (local + Docker)

    docker compose up -d --build frontend   # http://localhost:8080

Or deploy to Vercel:

    cd digital-farm-lab-frontend
    vercel --prod

Set `SPRING_API_URL` to the Render backend, and nginx (in Vercel's edge) will proxy `/api/*`.

## Repository structure

    DigitalFarmLab/
    ├── digital-farm-lab-backend/     Spring Boot + PostgreSQL
    │   ├── src/main/java/...         controllers, services, entities
    │   ├── src/main/resources/
    │   │   ├── application.yml
    │   │   ├── application-docker.yml
    │   │   └── db/migration/          V1 through V8
    │   ├── src/test/...              12 unit tests
    │   ├── Dockerfile
    │   └── AGENTS.md
    ├── digital-farm-lab-frontend/    React + Vite + PWA
    │   ├── src/pages/                6 sections
    │   ├── src/components/           EngineModal, ReflectionPanel, SensorPanel
    │   ├── public/                   PWA icons
    │   ├── Dockerfile
    │   └── nginx.conf.template
    ├── docker-compose.yml
    ├── .env.example
    └── README.md (this file)

## Security

- Passwords hashed (BCrypt via Spring Security)
- All queries parameterised (JPA / JdbcTemplate)
- JWT verified on every request; `@PreAuthorize` on role-gated routes
- Owner-scoped repos: `findByOwnerId…`, no cross-user data leaks
- Secrets in environment variables only; `.env` and `application-*.yml` gitignored
- No real personal data — synthetic only (POPIA-aligned)

## License

Educational use only. TUT 4IR Research Lab · WIL Full-Stack Programme.