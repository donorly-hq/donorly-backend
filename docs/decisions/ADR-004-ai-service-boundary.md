# ADR-004: The AI service never reads business tables

Status: Accepted (governs donorly-ai design)

## Context

If the AI service queried business data directly, tenant isolation would be enforced in two
codebases; a bug in either could leak one organization's donors to another. This is the
highest-consequence failure mode in the system.

## Decision

- Spring Boot assembles tenant-scoped context and passes it to donorly-ai per request,
  along with an explicit `organizationId`.
- donorly-ai owns only its own tables (embeddings, later eval runs) and filters every
  query by the passed `organizationId`.
- The LLM never generates or executes SQL. Financial mutations and mass communications
  always require human approval.
- Service-to-service auth: Cloud Run ID tokens; donorly-ai rejects unauthenticated traffic.

## Consequences

- Tenancy, permissions (`ai.use`/`ai.admin`), audit, and the org AI toggle stay in one place.
- Some duplication: context assembly code lives in Spring Boot even for AI features.
- Cross-tenant leakage tests are part of every AI feature's release gate.
