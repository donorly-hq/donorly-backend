# Code Review — July 2026

Full-codebase review of `donorly-backend` (Spring Boot) and `donorly-portal` (Next.js),
covering security, tenant isolation, code smells, duplication, dependency vulnerabilities,
and dead code. Severity uses Blocker / High / Medium / Low.

## How to read this

The single most important framing: **the backend is the security boundary.** The portal's
"High" findings are mostly defense-in-depth — they matter, but a bug in the backend is a
real breach while a bug in the portal is usually only a breach if the backend also failed.
Fix backend authorization gaps first.

---

## BLOCKER

### B1. Demo accounts with known password seeded on every startup
`config/DataSeeder.java` (~238–296), `application.properties:53` (`donorly.bootstrap.enabled=true`).
Seeds `owner@demo.donorly.org` … `volunteer@demo.donorly.org`, all with password `Demo1234!`.
On a fresh production DB an attacker signs in as org owner immediately.
**Fix:** gate `seedDemoUsers()` behind a non-prod Spring profile; set `bootstrap.enabled=false` in prod.
**Action needed:** confirm whether these demo rows exist in the production database right now.

---

## HIGH

### H1. Super-admin generated password written to logs (backend)
`DataSeeder.java:201–205` logs the generated super-admin password. Anyone with Cloud Run /
Railway / CI log access becomes platform super admin. **Fix:** never log it; emit a one-time
setup link or require `DONORLY_SUPERADMIN_PASSWORD` to be set out-of-band.

### H2. Org-wide financial dashboard requires only authentication (backend)
`DashboardController` `GET /` and `GET /my` use `@PreAuthorize("isAuthenticated()")`.
Any member incl. `volunteer` reads org totals (pledged, collected, donor counts).
**Fix:** require `reports.view` (or a dashboard permission).

### H3. Pledge CSV export not role-scoped (backend)
`ExportController.pledges()` gated only by `pledges.read`, which `volunteer` has; returns ALL
org pledges with donor names, bypassing the assigned-donor restriction volunteers have elsewhere.
**Fix:** require `donors.export`/`reports.view`, or scope the export to assigned donors.

### H4. Inventory assignment accepts users from other orgs (backend)
`InventoryService` assign path validates `holderUserId` only with `userRepository.findById`,
not org membership (contrast `VolunteerService.assign`). Cross-tenant integrity bug.
**Fix:** validate via `membershipRepository.findByOrganizationIdAndUserId`.

### H5. Financial concurrency: lost updates and duplicate receipt numbers (backend)
`PaymentService`: `collectedAmount` read-modify-write (two concurrent payments overwrite each
other) and receipt number via `COUNT(*)+1` (not atomic → duplicate receipt numbers on event
nights). **Fix:** atomic `UPDATE … SET collected_amount = collected_amount + ?` or row lock;
DB sequence / dedicated table for receipt numbers.

### H6. `PledgeUpdateRequest` can set `collectedAmount` directly (backend)
`PledgeService` update lets a client set `collectedAmount`, bypassing PaymentService validation,
receipts, and audit. **Fix:** remove that field from the update path; collected only changes via payments.

### H7. No automated tests (backend)
One `@SpringBootTest` context-load test; zero unit/integration/tenant-isolation tests.
Every isolation and money rule above is unprotected against regression. **Fix:** this is what
Stage 1–2 of the roadmap addresses; the acceptance criteria in REQ-004 become the first tests.

### H8. Dependency: Next.js 14.2.35 has known high-severity advisories (portal)
`npm audit`: `next` flagged High, `postcss` Moderate (XSS in stringify). **Fix:** bump Next.js
to a patched 14.2.x and postcss ≥ 8.5.10. Test the build; Next major bumps can break the App Router.

### H9. Portal defense-in-depth gaps (portal) — real risk only if backend authz is incomplete
- JWT in `localStorage` → any XSS = token theft (`lib/session.ts`).
- No `middleware.ts`; route protection is client-only (`(app)/layout.tsx`).
- Catch-all API proxy forwards any path + client headers with no allowlist (`api/[...path]/route.ts`).
- Pages fetch sensitive data before/without permission gates: platform orgs, audit, reports,
  year-end statements (all donors' PII), event registrations, insights.
**Fix:** page-level permission guards before fetch; proxy path allowlist; consider HttpOnly cookie
+ middleware. These reduce blast radius but the backend must still 403 every mutation.

---

## MEDIUM (condensed)

Backend:
- M1. Write paths accept unvalidated body FK IDs (follow-ups `donorId/campaignId/assignedToUserId`,
  event registration `donorId`, campaign `managedByUserId`, townhall `hostAmbassadorUserId`).
- M2. OTP codes stored plaintext (`AuthToken`); invite raw token returned in API response (`TeamService`).
- M3. Auth challenge endpoints (`/verify-otp`, `/select-org`, `/reset-password`) lack IP rate limiting;
  rate limiter is in-memory per-instance so effective limit scales with instance count.
- M4. Public check-in endpoints unthrottled (enumeration of check-in codes).
- M5. Logo upload: no size/dimension limit before base64 decode → memory/DB DoS.
- M6. `GlobalExceptionHandler` returns raw `ex.getMessage()` on 500 (info leak).
- M7. Sensitive data in logs: SMS body + recipient (`MessageDeliveryService`), AI prompt prefix
  (`AiGateway`), emails on auth events.
- M8. `InventoryController.delete()` gated by `inventory.assign` not `inventory.write` (copy-paste).
- M9. N+1 queries on most list endpoints and dashboards (per-row `findById`); some load whole-org
  collections to filter/count in memory. Follow the `InventoryService`/`AuditLogController`
  batch `findAllById` pattern.
- M10. Controllers return JPA entities directly (Pledge, Campaign, Event, FollowUp, Donor…) —
  couples API to schema, risks over-exposure.

Portal:
- M11. Client-side permissions tamperable (UI-only); stale session kept if `/auth/me` fails.
- M12. ~600 lines of duplicated platform-admin CRUD between `dashboard/page.tsx` and
  `platform/organizations/page.tsx`; repeated pagination/table markup across 4+ pages.
- M13. Public/invite pages send stale JWT by default (`auth` defaults true) → 401 redirect to login
  on public pages; pass `auth:false`.
- M14. `.env.local.example` advertises unused `NEXT_PUBLIC_API_BASE` (misconfig risk).

---

## LOW (condensed)

Backend: public thermometer/self-pledge create real records keyed only by campaign UUID (by design,
document it); sub-resource list endpoints return `[]` instead of 404 for wrong-org parents; AI sends
donor PII to OpenAI (DPA/consent review); unused `AiConversationRepository` method; scratch SQL dumps
at repo root (`donorly_complete_schema.sql`, `demo_data.sql`) duplicate Flyway.

Portal: XSS surface clean (no `dangerouslySetInnerHTML`); hardcoded hex colors bypass the brand tokens;
unused `OrganizationSummary` type; missing useEffect cleanup on some fetches; Google Fonts from CDN.

---

## Duplication hot-spots (fix once, reuse)

- `recomputeDonorLifetimeGiving` — identical in `PaymentService` and `PledgeService`.
- `findExistingDonor` donor-dedup — duplicated in `PledgeService` and `PublicPortalService`,
  both O(n) over all org donors.
- Campaign progress / thermometer aggregation — `PublicPortalService` vs `PledgeService`.
- Org member → DTO mapping — `TeamService` vs `OrganizationService`.
- Pagination boilerplate — 5 services.
- Portal platform-admin CRUD — 2 pages, ~600 lines.

Suggested extractions: `DonorMatchingService`, `DonorLifetimeGivingService`,
`CampaignProgressService`, `PaginationHelper` (backend); shared `usePagination` hook +
`PlatformOrgAdmin` module (portal).

---

## What is genuinely healthy

- ID-based reads consistently use `findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())`.
  Core tenant isolation on reads is solid.
- Tenant org comes from the JWT claim, never from request bodies on data endpoints.
- Money is `BigDecimal` everywhere; no float/double for amounts.
- JWT + server-side session (`jti`) allows revocation; BCrypt passwords; invite tokens SHA-256 hashed;
  `passwordHash` is `@JsonIgnore`; Flyway-owned schema with Hibernate `validate`.
- No SQL injection found (bound parameters; native-query table names from a static allowlist).
- Portal: centralized `api` wrapper; backend URL not baked into client bundle; permission-aware sidebar.

---

## Suggested fix ordering

1. Confirm + neutralize B1 (demo accounts) and H1 (password logging) in production now.
2. Backend authorization gaps H2, H3, H4 (small, high-impact).
3. Financial integrity H5, H6.
4. Dependency bumps H8.
5. Establish tests H7 (this is Roadmap Stage 1–2; REQ-004 criteria seed the first tenant-isolation tests).
6. Portal defense-in-depth H9, then Medium duplication/query cleanups opportunistically.
