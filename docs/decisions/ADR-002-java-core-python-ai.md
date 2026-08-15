# ADR-002: Java owns the core platform; Python owns AI orchestration

Status: Accepted (AI service not yet built)

## Context

The transactional core (auth, tenancy, donations, audit) must be deterministic and stable.
AI features are probabilistic, iterate weekly, and the AI tooling ecosystem (OpenAI SDK,
structured outputs, evaluation frameworks) is strongest in Python.

## Decision

- Spring Boot remains the authoritative core and the only service the portal calls.
- A separate `donorly-ai` service (Python + FastAPI, Cloud Run) owns prompt orchestration,
  embeddings, retrieval, structured outputs, and evaluation.
- Architecture is pulled in by features: the service is built when the first retrieval
  feature is built, not before.

## Consequences

- AI experiments cannot destabilize the money path; independent deploy cadence.
- Cost: a second service to deploy, authenticate (ID tokens), and observe.
- Explicitly rejected for now: Kafka, Kubernetes, dedicated vector DBs, multi-agent
  frameworks — complexity must be justified by real requirements.
