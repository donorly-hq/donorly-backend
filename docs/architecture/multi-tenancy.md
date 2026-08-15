# Multi-Tenancy Model

> Status: CURRENT. The single most security-critical design in Donorly.

## Model: shared database, shared schema, row-level scoping

Every tenant-owned table carries `organization_id`. Isolation is enforced in the application
layer: services resolve the caller's organization from the JWT (TenantContext) and every
repository query filters by it. There is no cross-tenant query path exposed to users.

## Membership and roles

- A user can belong to multiple organizations (`organization_memberships`), each with one role.
- Multi-org users choose their organization at login (org-select token flow, migration V13).
- Roles carry permission codes (`donors.read`, `ai.use`, `inventory.assign`, ...) seeded by `DataSeeder`;
  enforced via `@PreAuthorize` on controllers.
- Platform super admin (`is_platform_admin`) sits above organizations and requires email OTP at login.

## Rules for new code

1. Every new tenant-owned table MUST have `organization_id NOT NULL` plus an index on it.
2. Every new query path MUST filter by the TenantContext org — never trust an org id from the request body.
3. The future AI service receives `organizationId` explicitly on every call and filters
   embeddings queries by it; it never joins business tables (ADR-004).

## Known follow-ups

<!-- TODO (Zahed): after the Stage 1 ownership review, list here anything you found
     that does not follow the rules above (candidates for hardening work). -->
