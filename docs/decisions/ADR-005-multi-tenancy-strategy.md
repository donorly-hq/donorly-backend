# ADR-005: Shared schema with row-level organization scoping

Status: Accepted (in production)

## Context

Donorly serves many small organizations (mosques, nonprofits). Options considered:
database-per-tenant, schema-per-tenant, shared schema with row scoping.

## Decision

Shared database and schema; every tenant-owned row carries `organization_id`; the
application layer (TenantContext resolved from the JWT) filters every query.
See `docs/architecture/multi-tenancy.md` for the enforced rules.

## Consequences

- Cheapest to operate and migrate (one Flyway history); onboarding a new org is an INSERT.
- Cross-tenant safety depends on application discipline — mitigated by code review rules
  and (future) automated cross-tenant tests.
- Rejected: database-per-tenant (operational cost at hundreds of orgs, painful migrations);
  schema-per-tenant (Flyway complexity, connection pool fragmentation).
- Revisit if: a tenant requires hard isolation contractually, or row counts make shared
  indexes a bottleneck.
