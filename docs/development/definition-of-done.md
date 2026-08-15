# Definition of Done

A feature is done when ALL of the following hold:

1. An approved REQ exists and every acceptance criterion passes.
2. An approved PLAN exists and the implementation matches it (deviations documented).
3. Tests: unit tests for logic, integration tests for endpoints, authorization tests
   (wrong role is rejected), tenant-isolation tests (Org A cannot touch Org B).
4. Flyway migration applied cleanly and is backward-safe (no destructive change without a plan).
5. No secrets in code or logs; new secrets live in Secret Manager.
6. Audit logging covers new state-changing actions.
7. For AI features additionally: evidence cited in outputs, cost/latency logged,
   golden-dataset eval passing, cross-tenant adversarial tests passing.
8. Documentation updated: architecture/ADR/feature docs that the change makes stale.
9. LEARN-XXX log written; Zahed can explain the design without AI assistance.
10. Deployed to production and smoke-tested.
