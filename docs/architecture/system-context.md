# System Context

> Status: CURRENT as of July 2026. This documents what exists, not what is planned.

## Components in production

| Component | Tech | Hosting | Notes |
|---|---|---|---|
| donorly-portal | Next.js 14 (App Router), TypeScript, Tailwind | Cloud Run (us-central1) | PWA-enabled; proxies `/api/*` to backend |
| donorly-backend | Spring Boot 4.0.x, Java, JPA, Flyway | Cloud Run (us-central1) | The core platform; owns all business logic |
| Database | PostgreSQL | Railway | Flyway migrations V1–V14; schema in `src/main/resources/db/migration/` |
| Secrets | Google Secret Manager | GCP | `donorly-jwt-secret`, `donorly-db-password`, `donorly-mail-password`, `donorly-openai-key` |
| Email | Gmail SMTP (Google Workspace) | — | Sends from sam@donorly.com; invitations, OTP, password reset |
| CI/CD | Cloud Build (`cloudbuild.yaml` in each repo) | GCP | Build image → Artifact Registry → deploy to Cloud Run |

## Planned (approved direction, not yet built)

- `donorly-ai` — Python + FastAPI AI service on Cloud Run (private, ID-token auth).
  Owns prompt orchestration, embeddings/pgvector, structured outputs. Never reads business tables.
  See ADR-002 and ADR-004.

## Data flow (current)

Portal → (JWT in Authorization header) → Spring Boot REST API → PostgreSQL.
AI calls currently go Spring Boot `AiGateway` → OpenAI directly; this moves to donorly-ai per ADR-004.

## Key backend packages

- `controller/` — REST endpoints, `@PreAuthorize` permission checks
- `service/` — business logic, tenant scoping via TenantContext
- `repository/` — Spring Data JPA
- `model/` — JPA entities; audit via `AuditEntityListener`
- `config/` — security, seeding (`DataSeeder`), CORS

<!-- TODO (Zahed): draw the container diagram by hand once (paper is fine),
     then verify it against this file. If you can't draw it from memory, that's
     the gap Stage 1 ownership reviews are for. -->
