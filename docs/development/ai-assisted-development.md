# AI-Assisted Development Rules

The danger is not AI assistance; it is letting product progress exceed understanding.

## Delegation policy by area

| Area | Policy |
|---|---|
| Java boilerplate, repetitive tests | AI automates freely |
| Spring Boot domain logic | AI assists; Zahed reviews and must be able to explain |
| Architecture decisions | AI challenges; Zahed decides (recorded as ADRs) |
| PostgreSQL schema | AI assists; Zahed deeply reviews every migration |
| Python / FastAPI (while learning) | Zahed writes; AI is tutor and reviewer only |
| RAG, embeddings, agents | Explain → plan → review before any AI automation |
| Security, authorization, tenancy | Deep human review mandatory, always |

## The eight questions

Before approving any AI-produced component, Zahed must be able to answer:
1. Why does this component exist?
2. What calls it and what does it call?
3. What data does it read or modify?
4. What happens when it fails?
5. How is it authorized?
6. How is tenant isolation enforced?
7. How is it tested?
8. Why this design over the alternatives?

## Lifecycle

No code without an approved PLAN. No PLAN without an approved REQ. Every feature ends
with a LEARN log and a quiz session before it counts as done.
