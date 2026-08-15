# Davey Scaffolding Management System — Project Plan

This plan breaks the v1 system described in the [architecture document](../arch/architecture.md) into small, independently deliverable tickets. Each ticket is deliberately kept as small as possible so it can be developed, reviewed, and merged on its own.

Tickets are grouped into milestones and ordered by dependency: each ticket only depends on tickets in the same or earlier milestones. Dependencies are listed explicitly where they exist.

To mirror these tickets as GitHub issues (one issue per ticket, labelled `milestone-N`), run [`scripts/create-issues.sh`](../../scripts/create-issues.sh) with an authenticated [GitHub CLI](https://cli.github.com). The script is idempotent — re-running it skips tickets that already have an issue.

## Milestone 0 — Repository scaffolding

### DAV-1: Create Maven parent POM
- Add root `pom.xml` (Maven multi-module parent) with Java 21, Spring Boot 3.x BOM, and no modules yet.
- Add the Maven wrapper (`./mvnw`).
- **Done when**: `./mvnw validate` succeeds locally.

### DAV-2: Add root `.gitignore` and `.editorconfig`
- Ignore Maven/IDE/Node build outputs.
- **Done when**: build artifacts are not picked up by `git status`.

### DAV-3: Set up CI skeleton (GitHub Actions)
- Workflow that runs `./mvnw verify` on every push/PR.
- **Depends on**: DAV-1.
- **Done when**: the workflow runs green on a PR.

## Milestone 1 — Job Service (core of v1)

### DAV-4: Scaffold job-service module
- Create `services/job-service` Maven module (Spring Web, Spring Data JPA, Flyway, Bean Validation, Actuator) registered in the parent POM.
- Application boots with a `/actuator/health` endpoint; no domain code yet.
- **Depends on**: DAV-1.
- **Done when**: `./mvnw -pl services/job-service test` passes and the app starts.

### DAV-5: Job entity and Flyway migration
- Add the `Job` JPA entity per the architecture data model (§4.3) and `V1__create_jobs_table.sql`.
- Add a `@DataJpaTest` with Testcontainers Postgres verifying the migration and mapping.
- **Depends on**: DAV-4.
- **Done when**: the Testcontainers test passes.

### DAV-6: Job repository and create endpoint
- `POST /api/v1/jobs` with request validation (required `customerName`, `siteAddress`) returning `201` + the created job.
- Unit tests for validation; `MockMvc` test for the controller.
- **Depends on**: DAV-5.
- **Done when**: tests pass and invalid input returns `400`.

### DAV-7: Get and list jobs endpoints
- `GET /api/v1/jobs/{id}` (404 when missing) and `GET /api/v1/jobs` (unfiltered list).
- **Depends on**: DAV-6.
- **Done when**: `MockMvc` tests pass.

### DAV-8: List filtering by status and paid
- Support `GET /api/v1/jobs?status=...&paid=...` query parameters.
- **Depends on**: DAV-7.
- **Done when**: filter combinations are covered by tests.

### DAV-9: Update job endpoint
- `PUT /api/v1/jobs/{id}` with the same validation as create; `updatedAt` maintained by the service.
- **Depends on**: DAV-6.
- **Done when**: tests pass, including 404 and 400 cases.

### DAV-10: Delete job endpoint
- `DELETE /api/v1/jobs/{id}` returning `204` (404 when missing).
- **Depends on**: DAV-6.
- **Done when**: tests pass.

## Milestone 2 — Auth Service

### DAV-11: Scaffold auth-service module
- Create `services/auth-service` Maven module (Spring Web, Spring Security, Spring Data JPA, Flyway, Actuator) registered in the parent POM.
- **Depends on**: DAV-1.
- **Done when**: the app boots and `/actuator/health` returns 200.

### DAV-12: User entity, migration, and bcrypt storage
- `users` table Flyway migration, JPA entity, and `BCryptPasswordEncoder` wiring.
- Seed mechanism for the initial owner account (via migration or env-driven bootstrap — no plaintext secrets in the repo).
- **Depends on**: DAV-11.
- **Done when**: a `@DataJpaTest` verifies the schema and a unit test verifies password hashing.

### DAV-13: Login endpoint issuing JWTs
- `POST /api/v1/auth/login` validating credentials and returning a signed JWT access token (signing key from environment).
- Unit tests for token claims/expiry; `MockMvc` tests for success and wrong-credentials (401).
- **Depends on**: DAV-12.
- **Done when**: tests pass.

### DAV-14: Refresh endpoint
- `POST /api/v1/auth/refresh` exchanging a valid refresh token for a new access token.
- **Depends on**: DAV-13.
- **Done when**: tests cover valid, expired, and invalid refresh tokens.

## Milestone 3 — API Gateway

### DAV-15: Scaffold gateway module
- Create `gateway` Maven module using Spring Cloud Gateway, registered in the parent POM.
- **Depends on**: DAV-1.
- **Done when**: the app boots and `/actuator/health` returns 200.

### DAV-16: Static routes to services
- YAML routes: `/api/v1/jobs/** → job-service`, `/api/v1/auth/** → auth-service` (Docker Compose service names).
- **Depends on**: DAV-15.
- **Done when**: route predicate unit tests pass.

### DAV-17: JWT validation at the gateway
- Configure Spring Security OAuth2 resource-server JWT validation; `/api/v1/auth/login` and `/api/v1/auth/refresh` stay public, everything else requires a valid token.
- **Depends on**: DAV-16, DAV-13.
- **Done when**: tests show 401 without/with an invalid token and pass-through with a valid one.

### DAV-18: JWT validation in job-service (defence in depth)
- Job-service also validates JWTs (resource-server config) per §5.1.
- **Depends on**: DAV-6, DAV-13.
- **Done when**: job endpoints return 401 without a valid token in tests.

### DAV-19: Rate limiting at the gateway
- Add basic rate limiting on gateway routes.
- **Depends on**: DAV-16.
- **Done when**: excess requests receive `429` in a test.

## Milestone 4 — Local runtime (Docker Compose)

### DAV-20: Jib image builds
- Configure the Jib Maven plugin for job-service, auth-service, and gateway.
- **Depends on**: DAV-4, DAV-11, DAV-15.
- **Done when**: `./mvnw jib:dockerBuild` produces images for all three services.

### DAV-21: docker-compose.yml with Postgres
- Compose file with a single Postgres 16 container, init script creating `jobs_db`/`users_db` with per-service users, plus the three service containers with health checks and heap caps (`-Xmx128m`).
- **Depends on**: DAV-20.
- **Done when**: `docker compose up -d --wait` brings the full stack up healthy on a laptop.

## Milestone 5 — Integration tests (Python)

### DAV-22: IT test harness
- Add `it-tests/requirements.txt` (Python 3.12, pytest, requests) and `it-tests/conftest.py` with shared base-URL/env-var fixtures (§5.4.2).
- **Depends on**: DAV-21.
- **Done when**: `pytest it-tests/` collects successfully.

### DAV-23: job-service IT suite
- `it-tests/job-service/`: CRUD, status/paid transitions, validation errors, filtering — run against `docker compose up -d --wait job-service postgres`.
- **Depends on**: DAV-22, DAV-8, DAV-9, DAV-10.
- **Done when**: the suite passes against the running service.

### DAV-24: auth-service IT suite
- `it-tests/auth-service/`: login issues a JWT with expected claims/expiry; wrong credentials rejected; refresh flow works.
- **Depends on**: DAV-22, DAV-14.
- **Done when**: the suite passes against the running service.

### DAV-25: gateway IT suite
- `it-tests/gateway/`: routing forwards correctly; missing/invalid JWT rejected (401); health endpoint healthy.
- **Depends on**: DAV-22, DAV-17.
- **Done when**: the suite passes against the running gateway.

### DAV-26: Whole-system IT suite
- `it-tests/system/`: auth flow end-to-end, job lifecycle through the gateway as a logged-in user, all health endpoints green (§5.4.2).
- **Depends on**: DAV-23, DAV-24, DAV-25.
- **Done when**: the suite passes against the full compose stack.

### DAV-27: Wire IT suites into CI
- Extend the GitHub Actions workflow per §5.4.3: unit tests → Jib build → per-service IT matrix → whole-system IT; dump compose logs on failure.
- **Depends on**: DAV-3, DAV-26.
- **Done when**: all layers run green in CI.

## Milestone 6 — Frontend

### DAV-28: Scaffold frontend app
- Create `frontend/` with React + TypeScript + Vite; placeholder page; `npm run build` produces static output.
- **Done when**: the app builds and runs locally.

### DAV-29: Login screen
- Login form calling `POST /api/v1/auth/login` via the gateway; store token and attach it to subsequent API calls; redirect to job list on success.
- **Depends on**: DAV-28, DAV-17.
- **Done when**: a user can log in against the local compose stack.

### DAV-30: Job list view
- List jobs with **completed**/**paid** badges and status/paid filters.
- **Depends on**: DAV-29, DAV-8.
- **Done when**: the list renders live data with working filters.

### DAV-31: Job detail view
- Read-only view of a single job.
- **Depends on**: DAV-30.
- **Done when**: navigating from the list shows a job's details.

### DAV-32: Create and edit job forms
- Forms for creating and updating jobs with client-side validation mirroring the API rules.
- **Depends on**: DAV-31, DAV-9.
- **Done when**: jobs can be created and edited end-to-end.

### DAV-33: Delete job with confirmation
- Delete action with a confirmation dialog.
- **Depends on**: DAV-31, DAV-10.
- **Done when**: a job can be deleted from the UI.

### DAV-34: Frontend build in CI
- Add lint/build (and any tests) for `frontend/` to the CI workflow.
- **Depends on**: DAV-3, DAV-28.
- **Done when**: frontend checks run green in CI.

## Milestone 7 — Production deployment (v1)

### DAV-35: Publish images to GHCR
- CI step pushing versioned service images to GitHub Container Registry after all tests pass.
- **Depends on**: DAV-27.
- **Done when**: images appear in GHCR from a main-branch build.

### DAV-36: VPS setup runbook
- Documented, repeatable server-setup steps (Docker, compose, firewall, non-root user) in `docs/` per §5.3.
- **Done when**: a fresh VPS can be prepared by following the doc.

### DAV-37: Caddy reverse proxy with HTTPS
- Add `Caddyfile` and Caddy service to compose for TLS termination with automatic Let's Encrypt certificates.
- **Depends on**: DAV-21, DAV-36.
- **Done when**: the API is reachable over HTTPS on the VPS.

### DAV-38: CD deploy job
- GitHub Actions job that SSHes to the VPS and runs `docker compose pull && docker compose up -d` after images are published.
- **Depends on**: DAV-35, DAV-37.
- **Done when**: a merge to main deploys automatically.

### DAV-39: Frontend production hosting
- Deploy the built SPA to Cloudflare Pages or GitHub Pages, pointed at the production API; configure CORS accordingly.
- **Depends on**: DAV-34, DAV-37.
- **Done when**: the app is usable from the public URL.

### DAV-40: Nightly database backups
- Cron-driven `pg_dump` shipped to free/cheap object storage (R2/B2); document the restore procedure.
- **Depends on**: DAV-37.
- **Done when**: a backup exists off-box and a restore has been tested once.

## Milestone 8 — Observability hardening

### DAV-41: Structured JSON logging with correlation IDs
- JSON log output in all services; gateway generates/propagates a request ID header; services log it.
- **Depends on**: DAV-17.
- **Done when**: a single request can be traced across gateway and service logs.

### DAV-42: Basic metrics per service
- Expose request rate, error rate, and latency metrics (Actuator/Micrometer) from each service.
- **Depends on**: DAV-4, DAV-11, DAV-15.
- **Done when**: metrics endpoints report the three signals for each service.
