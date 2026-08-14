# Davey Scaffolding Management System — Architecture

## 1. Overview

Davey Scaffolding is a small scaffolding business based in London. This document describes the architecture of a web-based system that allows the owner to manage the business's jobs.

### 1.1 Goals (in scope)

- View all jobs in one place.
- See at a glance which jobs are **completed** and which are **paid**.
- Create, read, update, and delete (CRUD) jobs.
- Simple, secure access for a very small number of users (initially one).

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

- Single-page application (SPA) served as static assets.
- Talks only to the API Gateway over HTTPS.
- Responsibilities:
  - Job list view with filters/badges for **completed** and **paid** status.
  - Job detail view.
  - Forms for creating and editing jobs; delete with confirmation.
  - Login screen.

### 4.2 API Gateway

- Single public entry point for all API traffic.
- Responsibilities:
  - TLS termination.
  - Request routing to backend services (`/api/v1/jobs/** → Job Service`, `/api/v1/auth/** → Auth Service`).
  - Authentication enforcement (validates JWTs issued by the Auth Service).
  - Rate limiting and basic request validation.

### 4.3 Job Service

The core service for this release. Owns the job domain and its data.

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

- Issues JWT access tokens; validation is performed by the API Gateway (and by services for defence in depth — see §5.1) using the token signing key.
- Manages the (small) set of user accounts; passwords stored using a strong adaptive hash (e.g. bcrypt/argon2).
- Endpoints: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`.

### 4.5 Databases

- One database per service (Jobs DB, Users DB) to preserve service autonomy.
- Relational databases (e.g. PostgreSQL) — the data is small and strongly structured.
- No service accesses another service's database directly; all cross-service access goes through APIs.

## 5. Cross-cutting concerns

### 5.1 Security

- All traffic over HTTPS.
- JWT-based authentication enforced at the gateway; services also validate tokens (defence in depth).
- Secrets (DB credentials, signing keys) provided via environment/secret manager — never committed to the repo.
- Input validation at service boundaries.

### 5.2 Observability

- Structured (JSON) logging from every service with a correlation/request ID propagated through the gateway.
- Health-check endpoint (`/healthz`) on every service.
- Basic metrics (request rate, error rate, latency) per service.

### 5.3 Deployment

- Each service is packaged as a Docker container.
- CI (GitHub Actions) builds, tests, and publishes images per service from this monorepo.
- Target runtime: a small managed container platform (e.g. a single-node container host or managed container service) — the workload is tiny, so cost and simplicity outweigh elasticity.

### 5.4 Monorepo layout (proposed)

```
/
├── docs/
│   └── arch/               # this document
├── services/
│   ├── job-service/
│   └── auth-service/
├── gateway/
└── frontend/
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
| REST/JSON APIs | Simple, well understood, easy to test | Less efficient than gRPC (irrelevant at this scale) |
| Database per service | Service autonomy, independent evolution | Cross-service queries require API calls |
| JWT auth at gateway | Central enforcement, stateless services | Token revocation needs short expiry + refresh flow |
| Synchronous calls only in v1 | Simplicity; no cross-service workflows yet | Eventing added later if/when needed |
