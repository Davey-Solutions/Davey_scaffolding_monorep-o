# Davey Scaffolding Management System — Architecture

## 1. Overview

Davey Scaffolding is a small scaffolding business based in London. This document describes the architecture of a web-based system that allows the owner to manage the business's jobs.

### 1.1 Goals (in scope)

- View all jobs in one place.
- See at a glance which jobs are **completed** and which are **paid**.
- Create, read, update, and delete (CRUD) jobs.
- Simple, secure access for a very small number of users (initially one).

#### 1.1.1 Constraints

- **Cost**: running costs must be as close to zero as possible until the system has proven its value (i.e. the owner actually uses it). Target: **≤ ~£5/month** all-in for v1. The architecture must still allow scaling up later without a rewrite.
- **Team**: built and operated by one developer; Java is the preferred backend language.

### 1.2 Out of scope (future work)

The following are explicitly out of scope for this initial version, but the architecture should not prevent them from being added later:

- Staff rota management.
- Invoice generation.

## 2. Architectural style

The system uses a **microservice architecture**, hosted in this monorepo. Each service:

- Owns its own data (database-per-service pattern).
- Exposes a versioned REST/JSON API.
- Is independently buildable, testable, and deployable as a container.

Given the small initial scope, the number of services is deliberately kept low. The service boundaries are chosen to match the planned future domains (jobs, staff/rotas, invoicing) so the system can grow without re-architecting.

### 2.1 Technology stack

Java is the preferred backend language, so the stack is standardised on the Java/Spring ecosystem — one language, one build tool, one set of idioms across every service.

| Layer | Choice | Why |
|-------|--------|-----|
| Language | **Java 21 (LTS)** | Preferred language; LTS gives free security updates for years; virtual threads make simple blocking code scale well |
| Service framework | **Spring Boot 3.x** (Spring Web, Spring Data JPA, Spring Security, Bean Validation) | De-facto standard for Java microservices; huge ecosystem; embedded server so each service is a self-contained jar |
| API Gateway | **Spring Cloud Gateway** | Same language/stack as the services; route config is plain YAML; built-in JWT validation via Spring Security's OAuth2 resource server support |
| Auth / JWT | **Spring Security** + Spring's OAuth2 resource-server JWT support; passwords hashed with **bcrypt** (`BCryptPasswordEncoder`) | No custom crypto; well-audited defaults |
| Database | **PostgreSQL 16** | Free, rock-solid, relational (the data is small and strongly structured); excellent Spring Data/JPA support |
| Schema migrations | **Flyway** | Versioned SQL migrations run automatically on service start-up |
| Build | **Maven** multi-module build (one module per service) | Single `./mvnw verify` for the whole monorepo; per-service artifacts; conventional, declarative builds with a huge ecosystem |
| Containerisation | **Docker**, images built with **Jib** (Maven plugin) | Jib builds small layered images without a Dockerfile or Docker daemon in CI |
| Testing (unit/service level) | **JUnit 5**, **Testcontainers** (real Postgres in service-level tests), Spring's `MockMvc`/`WebTestClient` | Tests run against the same DB engine as production (see §5.4) |
| Testing (integration/E2E) | **Python 3.12 + pytest + requests** against the running Docker Compose stack | Black-box tests through the real gateway; language-neutral and quick to write (see §5.4) |
| Frontend | **React + TypeScript + Vite**, static build output | Small SPA; builds to plain static files that can be hosted for free |
| CI/CD | **GitHub Actions** | Free for this repo's usage level; builds, tests, and publishes images to **GitHub Container Registry (GHCR)** (also free) |

Notes:

- **Memory footprint**: several JVMs on one small box is the main cost risk. Mitigations: cap each service's heap (e.g. `-Xmx128m` is ample for this workload), and if it ever gets tight, compile services to native binaries with **GraalVM native-image** (fully supported by Spring Boot 3) to cut RAM use dramatically. Not needed on day one.
- **No service mesh / discovery server**: with a handful of services on one host, static routing in the gateway (Docker Compose service names) is enough. Eureka/Consul etc. are deliberately omitted.

## 3. System context

```
+-----------+        HTTPS         +--------------+
|  Browser  | <------------------> |  Web Frontend|
| (Owner)   |                      |  (SPA)       |
+-----------+                      +------+-------+
                                          |
                                          v
                                   +--------------+
                                   | API Gateway  |
                                   +--+--------+--+
                                      |        |
                             +--------+        +---------+
                             v                           v
                      +-------------+             +-------------+
                      | Job Service |             | Auth Service|
                      +------+------+             +------+------+
                             |                           |
                             v                           v
                      +-------------+             +-------------+
                      |  Jobs DB    |             |  Users DB   |
                      +-------------+             +-------------+
```

## 4. Components

### 4.1 Web Frontend

- Single-page application (SPA) — React + TypeScript + Vite — served as static assets.
- Talks only to the API Gateway over HTTPS.
- Responsibilities:
  - Job list view with filters/badges for **completed** and **paid** status.
  - Job detail view.
  - Forms for creating and editing jobs; delete with confirmation.
  - Login screen.

### 4.2 API Gateway

- Single public entry point for all API traffic; implemented with **Spring Cloud Gateway**.
- Responsibilities:
  - Request routing to backend services (`/api/v1/jobs/** → Job Service`, `/api/v1/auth/** → Auth Service`) via static YAML routes.
  - Authentication enforcement (validates JWTs issued by the Auth Service).
  - Rate limiting and basic request validation.
- TLS termination is handled just in front of the gateway by the reverse proxy (Caddy — see §5.3), which manages Let's Encrypt certificates automatically.

### 4.3 Job Service

The core service for this release. Owns the job domain and its data. Implemented as a Spring Boot 3 application (Spring Web + Spring Data JPA + Flyway) against its own PostgreSQL database.

Responsibilities:

- CRUD operations on jobs.
- Tracking job status (completed / not completed) and payment status (paid / unpaid).
- Listing and filtering jobs (e.g. all unpaid jobs, all outstanding jobs).

#### API (v1)

| Method | Path                  | Description                          |
|--------|-----------------------|--------------------------------------|
| GET    | `/api/v1/jobs`        | List jobs (filter by `status`, `paid`) |
| POST   | `/api/v1/jobs`        | Create a job                         |
| GET    | `/api/v1/jobs/{id}`   | Get a single job                     |
| PUT    | `/api/v1/jobs/{id}`   | Update a job                         |
| DELETE | `/api/v1/jobs/{id}`   | Delete a job                         |

#### Data model

`Job`

| Field         | Type      | Notes                                    |
|---------------|-----------|------------------------------------------|
| `id`          | UUID      | Primary key                              |
| `customerName`| string    | Required                                 |
| `siteAddress` | string    | Required — where the scaffolding goes up |
| `description` | string    | Free text description of the work        |
| `price`       | decimal   | Quoted/agreed price (GBP)                |
| `status`      | enum      | `PENDING`, `IN_PROGRESS`, `COMPLETED`    |
| `paid`        | boolean   | Whether payment has been received        |
| `startDate`   | date      | Optional                                 |
| `endDate`     | date      | Optional                                 |
| `createdAt`   | timestamp | Set by the service                       |
| `updatedAt`   | timestamp | Set by the service                       |

### 4.4 Auth Service

- Spring Boot 3 application using Spring Security.
- Issues JWT access tokens; validation is performed by the API Gateway (and by services for defence in depth — see §5.1) using the token signing key.
- Manages the (small) set of user accounts; passwords stored using **bcrypt** (`BCryptPasswordEncoder`).
- Endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`.

### 4.5 Databases

- **PostgreSQL 16** for everything.
- **Logical** database-per-service: to keep costs down, v1 runs a **single Postgres instance** (one container) hosting a **separate database per service** (`jobs_db`, `users_db`), each with its own DB user that can only access its own database. This preserves service autonomy — no shared tables, no cross-database queries — while paying for one instance.
- If/when the system grows, each database can be lifted onto its own (or a managed) Postgres instance with no application changes beyond a connection string.
- Schema changes are applied by **Flyway** migrations owned by each service.
- No service accesses another service's database; all cross-service access goes through APIs.

## 5. Cross-cutting concerns

### 5.1 Security

- All traffic over HTTPS.
- JWT-based authentication enforced at the gateway; services also validate tokens (defence in depth).
- Secrets (DB credentials, signing keys) provided via environment/secret manager — never committed to the repo.
- Input validation at service boundaries.

### 5.2 Observability

- Structured (JSON) logging from every service with a correlation/request ID propagated through the gateway.
- Health-check endpoint (Spring Boot Actuator's `/actuator/health`) on every service.
- Basic metrics (request rate, error rate, latency) per service.

### 5.3 Deployment

The guiding principle is **prove it before paying for it**: v1 runs on the cheapest predictable setup that still looks and operates like a real production system.

#### v1: single small VPS + Docker Compose

- One small VPS (e.g. Hetzner CX22 at ~€4 (~£3.50)/month, or an equivalent ~£4–5/month box from DigitalOcean/OVH). 2 vCPU / 4 GB RAM is comfortably enough for four small JVM containers plus Postgres.
- Everything runs on that one box via a single **`docker-compose.yml`**:

```
                     VPS (~£5/month)
  +---------------------------------------------------+
  |  Caddy (reverse proxy, automatic HTTPS via        |
  |         Let's Encrypt — free certificates)        |
  |     |                                             |
  |     v                                             |
  |  api-gateway ──> job-service ──> postgres         |
  |        └───────> auth-service ──> (same instance, |
  |                                    separate DBs)  |
  +---------------------------------------------------+
```

- **Frontend**: the built static SPA is hosted for **free** on Cloudflare Pages or GitHub Pages (both have generous free tiers), calling the API on the VPS. Alternatively Caddy can serve it from the same box — also free.
- **TLS**: Caddy obtains and renews Let's Encrypt certificates automatically. Domain: a `.co.uk` domain is ~£10/year (or start on a free subdomain).
- **CI/CD**: GitHub Actions builds and tests on every push (unit, service-level, and Python integration tests — see §5.4), publishes images to GHCR (free), and deploys by SSH-ing to the VPS and running `docker compose pull && docker compose up -d`. No paid deployment tooling.
- **Backups**: nightly `pg_dump` via cron, shipped to free/cheap object storage (e.g. Cloudflare R2's free tier or Backblaze B2 — pennies at this data volume). Backups are the one thing that must exist from day one.
- **Local development**: the same `docker-compose.yml` runs the full system on a laptop, so dev and prod are identical.
- **Infrastructure as code**: **not needed for v1**. Terraform/Terragrunt earn their keep when there is real cloud infrastructure to manage (VPCs, load balancers, managed databases, multiple environments); here the entire infrastructure is one manually-provisioned VPS, and everything *on* the box is already declared as code in `docker-compose.yml` (plus the Caddyfile). A short, documented server-setup script (or the provider's one-click Docker image) is sufficient and keeps the toolchain small. If the system later moves to a cloud provider with more moving parts, plain **Terraform** can be introduced then — Terragrunt only becomes worthwhile with many environments/modules, which is far beyond this system's horizon.

Estimated running costs for v1:

| Item | Cost |
|------|------|
| VPS (all services + Postgres) | ~£4–5/month |
| Frontend hosting (Cloudflare/GitHub Pages) | £0 |
| TLS certificates (Let's Encrypt via Caddy) | £0 |
| CI + container registry (GitHub Actions + GHCR) | £0 |
| Backup storage (R2/B2 free tier) | ~£0 |
| Domain name | ~£10/year |
| **Total** | **~£5/month** |

#### Later: scaling up (only if usage justifies it)

The containerised, database-per-service design means growth is incremental, not a rewrite:

1. Move Postgres to a managed offering (e.g. Neon, Supabase, or a cloud provider's managed Postgres) for automated backups/HA.
2. Move containers to a managed container platform (e.g. Fly.io, or a small Kubernetes/ECS setup) when one box is no longer enough.
3. If JVM memory becomes the bottleneck before that, switch services to GraalVM native images (see §2.1).

### 5.4 Testing strategy

Testing follows the classic test pyramid: many fast unit tests, a solid layer of service-level tests, and black-box integration tests at two levels — per-service suites against each running service in isolation, and a whole-system suite that exercises the full stack exactly as the frontend does.

#### 5.4.1 Unit tests (Java, per service)

- **Tools**: JUnit 5 + Mockito + AssertJ, run by Maven (`./mvnw test`).
- **Scope**: business logic in isolation — services, validators, mappers — with repositories and external collaborators mocked. No Spring context, no database, no network; each test runs in milliseconds.
- **What gets covered**: job status/paid transitions, input validation rules, price handling, auth token issuance logic, gateway route predicates.
- **Where**: `src/test/java` inside each Maven module (`services/job-service`, `services/auth-service`, `gateway`).
- **Target**: the bulk of coverage lives here; a change to any service's logic should be provable without starting anything.

Service-level tests (still Java, still `./mvnw test`) sit just above: `@SpringBootTest`/`@DataJpaTest` slices with **Testcontainers** spinning up a throwaway PostgreSQL, verifying JPA mappings, Flyway migrations, and controller behaviour via `MockMvc` — one service at a time, no other services required.

#### 5.4.2 Integration tests (Python, against running servers)

A separate, language-neutral **Python** suite talks HTTP to **running** containers, exactly like a real client. It is split into two levels: **per-service** suites that exercise each service directly in isolation, and a **whole-system** suite that exercises the full stack through the API Gateway.

- **Tools**: Python 3.12, **pytest**, **requests** (plus `pytest` fixtures for setup/teardown). Dependencies pinned in `it-tests/requirements.txt`; no other Python tooling needed.
- **Layout**: one pytest package per service plus a system package, all under `it-tests/` (see §5.5): `it-tests/job-service/`, `it-tests/auth-service/`, `it-tests/gateway/`, and `it-tests/system/`. Shared fixtures/helpers live in `it-tests/conftest.py`.

**Per-service IT suites** — each service tested as a black box on its own:

- **How they run**: `docker compose up -d --wait <service> postgres` starts only the service under test and its database (no gateway, no other services), then `pytest it-tests/<service>/` runs against the service's own port (`IT_<SERVICE>_BASE_URL` env vars, e.g. `IT_JOB_SERVICE_BASE_URL`, defaulting to the service's compose port).
- **What gets covered per service**:
  - **job-service**: job CRUD, status/paid transitions, validation errors (400s), filtering (`?status=COMPLETED&paid=false`), Flyway-migrated schema behaving correctly against real Postgres.
  - **auth-service**: login issues a JWT with the expected claims/expiry; wrong credentials rejected; refresh flow works.
  - **gateway**: routing rules forward to configured targets; requests without/with an invalid JWT are rejected (401); the health endpoint reports healthy (200).
- **Why per service**: failures point directly at one service; suites run in parallel in CI; a service can be changed and re-verified without starting the whole stack.

**Whole-system IT suite** — the full stack as the frontend sees it:

- **How it runs**:
  1. `docker compose up -d --wait` starts the full stack (gateway, job-service, auth-service, Postgres) — the same compose file used for dev and prod.
  2. `pytest it-tests/system/` runs against the gateway's base URL (`IT_BASE_URL` env var, default `http://localhost:8080`).
  3. `docker compose down -v` tears everything down.
- **What gets covered** (cross-service journeys only — per-service detail lives in the suites above):
  - **Auth flow end-to-end**: login via the gateway returns a JWT; that token is accepted by the gateway when calling the job service; unauthenticated requests are rejected at the gateway.
  - **Job lifecycle through the gateway**: create → read → list → update → delete as a logged-in user.
  - **Health**: every service's `/actuator/health` reports healthy once the stack is up.
- **Test data isolation**: each test creates its own data via the API and cleans up after itself (or the suite runs against a throwaway compose project), so tests are order-independent and repeatable.
- **Why Python here**: the integration suites deliberately share no code with the services — a bug in shared code can't hide in both places. pytest + requests keeps these tests short, readable, and fast to write, and the suites double as executable documentation of each service's API and of the public API.

#### 5.4.3 CI wiring

GitHub Actions runs all layers on every push/PR, in order — cheap tests first:

1. `./mvnw test` — unit + service-level tests (Testcontainers Postgres runs fine on GitHub-hosted runners).
2. Build the service images (Jib).
3. **Per-service IT suites**: for each service, `docker compose up -d --wait <service> postgres` then `pytest it-tests/<service>/` (run as parallel matrix jobs).
4. **Whole-system IT suite**: `docker compose up -d --wait` using the freshly built images, then `pytest it-tests/system/`; compose logs are dumped as artifacts on failure.
5. Only if all layers pass are images pushed to GHCR and deployed.

All of this stays within GitHub Actions' free tier — no extra cost.

### 5.5 Monorepo layout (proposed)

```
/
├── docs/
│   └── arch/               # this document
├── services/
│   ├── job-service/        # Spring Boot (Maven module)
│   └── auth-service/       # Spring Boot (Maven module)
├── gateway/                # Spring Cloud Gateway (Maven module)
├── frontend/               # React + TypeScript + Vite
├── it-tests/               # Python (pytest + requests) integration tests
│   ├── requirements.txt
│   ├── conftest.py         # shared fixtures/helpers
│   ├── job-service/        # per-service IT suite (service + Postgres only)
│   ├── auth-service/       # per-service IT suite (service + Postgres only)
│   ├── gateway/            # per-service IT suite (routing/auth rules)
│   └── system/             # whole-system E2E suite through the gateway
├── docker-compose.yml      # full system, used for dev and v1 prod
└── pom.xml                 # Maven parent (multi-module build)
```

## 6. Future extensions

The service boundaries anticipate the planned roadmap:

- **Rota Service** — staff records and shift/rota scheduling. New service with its own DB; jobs may reference assigned staff by ID.
- **Invoice Service** — generates invoices from job data. Would consume job data via the Job Service API (or events), and marking an invoice as paid could update the job's `paid` flag via API call or event.
- **Eventing** — if inter-service coordination grows (e.g. "job completed" triggering invoice creation), introduce a lightweight message broker and publish domain events from the Job Service. Not needed for v1.

## 7. Decisions and trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| Microservices with few services | Requested style; boundaries match future domains | More operational overhead than a monolith for a small app |
| Java 21 + Spring Boot everywhere | Preferred language; one stack to learn and maintain; mature ecosystem | JVM memory footprint (mitigated by heap caps / GraalVM native if needed) |
| PostgreSQL for all services | Free, reliable, relational fits the data; first-class Spring support | None significant at this scale |
| Single Postgres instance, separate DB per service | Keeps v1 cost to one instance while preserving service data isolation | Shared failure domain until DBs are split onto their own instances |
| Single VPS + Docker Compose for v1 | ~£5/month total; identical setup for dev and prod | No high availability — acceptable for a single-user tool proving its value |
| Maven for the multi-module build | Declarative, conventional builds; ubiquitous tooling/CI support; first-class Spring Boot and Jib plugins | More verbose XML than Gradle; slightly slower builds (irrelevant at this repo size) |
| No Terraform/Terragrunt in v1 | Only infrastructure is one VPS; `docker-compose.yml` + Caddyfile already declare everything on the box; avoids extra tooling/state management | Server provisioning is documented/scripted rather than fully declarative; adopt Terraform later if cloud infra grows |
| Free static hosting for the SPA | £0; SPAs are just static files | Frontend deploys separately from backend |
| REST/JSON APIs | Simple, well understood, easy to test | Less efficient than gRPC (irrelevant at this scale) |
| Database per service | Service autonomy, independent evolution | Cross-service queries require API calls |
| JWT auth at gateway | Central enforcement, stateless services | Token revocation needs short expiry + refresh flow |
| Python (pytest + requests) for black-box integration tests | Independent of the Java stack (bugs can't hide in shared code); concise; doubles as API documentation | Second language in the repo, but confined to `it-tests/` |
| IT suites per service plus a whole-system suite | Failures point at one service; per-service suites parallelise in CI; the system suite covers cross-service journeys only | Some overlap in setup fixtures; slightly more CI orchestration than a single suite |
| Synchronous calls only in v1 | Simplicity; no cross-service workflows yet | Eventing added later if/when needed |
