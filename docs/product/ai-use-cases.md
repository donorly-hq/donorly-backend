# AI Use Cases

> Status: DRAFT — prioritized backlog of AI features, mapped to the AI maturity model.

## Maturity model (do not skip levels)

1. **Read-only assistant** — retrieves and explains
2. **Recommendation assistant** — suggests actions
3. **Drafting assistant** — prepares content and tasks
4. **Approval-based agent** — proposes executable actions, waits for approval
5. **Controlled automation** — runs pre-approved, low-risk workflows

## Tier 1 — plain LLM calls (maturity 1–3)

| Use case | Level | Module it plugs into |
|---|---|---|
| Draft-for-approval follow-up messages for overdue pledges | 3 | Follow-ups + Communications |
| Donor briefs for ambassadors (history, suggested ask, rationale) | 1 | Donors |
| Smart CSV import (AI maps messy columns to Donorly fields) | 2 | Donor import |

## Tier 2 — needs retrieval / RAG (maturity 1–3)

| Use case | Level | Module |
|---|---|---|
| Ask-anything over org history with citations | 1 | AI chat |
| Lapse detection + next-best-action weekly list | 2 | Follow-ups |

## Tier 3 — agentic (maturity 4)

| Use case | Level | Module |
|---|---|---|
| Weekly Fundraising Review (analysis + proposed actions + drafts, human approves) | 4 | New |
| Campaign forecasting and live event momentum cues | 2 | Campaigns / Event Mode |

## Hard rules for every use case

- AI drafts, humans send — anywhere money or donor relationships are touched.
- Every operational claim must cite evidence (pledge IDs, dates, notes).
- Every response is scoped to the authenticated organization. Cross-tenant leakage is a release-blocking test.
- The model never has unrestricted database access.
