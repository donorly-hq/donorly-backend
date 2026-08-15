# Donorly Documentation

This folder drives spec-driven, AI-assisted development. Markdown is the source of truth:
requirements are written and approved by a human before any code is planned or written.

## The lifecycle

IDEA → REQUIREMENT (you write) → AI PLAN (agent proposes) → HUMAN REVIEW → APPROVAL →
IMPLEMENTATION SLICE → TESTS → SECURITY REVIEW → LEARNING REVIEW → DOC UPDATE → MERGE → DEPLOY → OBSERVE

## Folder map

- `product/` — vision, AI use cases. What Donorly is and why.
- `architecture/` — how the system works today. Facts, not aspirations.
- `decisions/` — Architecture Decision Records (ADRs). Why we chose what we chose.
- `requirements/` — REQ-XXX files. WHAT and WHY. Owned and approved by Zahed.
- `plans/` — PLAN-XXX files. HOW. Proposed by AI, approved by Zahed before implementation.
- `development/` — working rules: definition of done, AI-assisted development agreement.
- `learning/` — career roadmap and LEARN-XXX logs proving skills were actually acquired.

## Rules

1. No implementation without an approved PLAN referencing an approved REQ.
2. REQ = what/why. PLAN = how. Never mix them.
3. Do not create empty placeholder files; add docs when the work exists.
4. Docs must not claim behavior that code and tests do not prove.
