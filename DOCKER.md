# Docker — full stack

Boots PostgreSQL + Spring Boot backend + Nginx-served React frontend with one command.

## Layout

    DigitalFarmLab/
    ├── docker-compose.yml           # stack orchestration
    ├── .env.example                 # env template (commit)
    ├── .env                         # real values (gitignored, generated)
    ├── digital-farm-lab-backend/
    │   ├── Dockerfile
    │   └── src/main/resources/application-docker.yml
    └── digital-farm-lab-frontend/
        ├── Dockerfile
        └── nginx.conf.template       # proxies /api with JWT injection

## What runs

| Service  | Image                     | Port | Health |
|----------|---------------------------|------|--------|
| postgres | postgres:18-alpine        | 5432 | pg_isready |
| backend  | temurin:21-jre (custom)   | 8081 | /actuator/health |
| frontend | nginx:alpine (custom)     | 80   | /healthz |

Backend has Flyway migrations applied automatically on first boot.

## First run

    # From DigitalFarmLab/ root
    docker compose up --build

First build takes ~5–10 min (downloads Gradle, Node modules, PostgreSQL).
Subsequent runs: `docker compose up` (no `--build` needed unless source changes).

## URLs

- Frontend:    http://localhost
- Backend:     http://localhost:8081
- Swagger UI:  http://localhost:8081/swagger-ui/index.html
- pgAdmin or psql: `localhost:5432`

## Verify

    docker compose ps                         # all three UP (healthy)
    curl http://localhost/healthz             # "ok"
    curl http://localhost/api/v1/workspace/farms -H "Authorization: Bearer <token>"
    docker compose logs backend | tail -20    # check "Tomcat started on port 8081"

## Stop

    docker compose down                       # keep data
    docker compose down -v                    # wipe PostgreSQL volume

## Rebuild a single service

    docker compose build backend              # rebuild backend image
    docker compose up -d backend              # restart just backend

## Changing the JWT

Edit `SPRING_DEV_TOKEN` in `.env`, then:

    docker compose up -d frontend             # picks up new token

## Troubleshooting

- **Port already in use** → change `FRONTEND_PORT` / `BACKEND_PORT` / `POSTGRES_PORT` in `.env`
- **Backend crashes with DB error** → `docker compose logs postgres` — check POSTGRES_* vars match
- **Frontend shows "Backend: ..." banner** → check `docker compose logs backend` for the real error
- **401 from /api/v1/** → JWT expired or secret mismatch between JWT_SECRET and SPRING_DEV_TOKEN