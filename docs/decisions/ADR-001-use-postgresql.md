# ADR-001: Use PostgreSQL as the single datastore

Status: Accepted (in production)

## Context

Donorly needs transactional integrity for money-adjacent records (pledges, payments, receipts),
relational queries across donors/campaigns/events, and later vector search for AI retrieval.

## Decision

PostgreSQL (currently hosted on Railway) for all business data, with Flyway owning schema
migrations. Vector search will use the pgvector extension in the same database (ADR-003)
rather than a separate datastore.

## Consequences

- One backup/restore story, one tenancy model, one connection pool.
- ACID transactions across pledge → payment → receipt.
- Scaling ceiling is a managed-Postgres ceiling; acceptable far beyond current scale.
- Alternatives rejected: separate vector DB (second tenancy model to secure), NoSQL (no
  transactional guarantees for financial records).
