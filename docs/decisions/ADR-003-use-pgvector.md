# ADR-003: Use pgvector for embeddings, not a dedicated vector database

Status: Accepted (pending verification that the extension is enabled on Railway)

## Context

RAG features need vector similarity search over donor notes, communications, and documents.
Donorly is multi-tenant; any datastore holding embeddings must enforce org isolation.

## Decision

Store embeddings in PostgreSQL using the pgvector extension, in tables owned by the AI
service, with `organization_id NOT NULL` on every row and every query filtered by it.

## Consequences

- Embeddings live inside the existing tenancy and backup model; no second datastore to secure.
- pgvector performance is sufficient for per-org corpus sizes (thousands of chunks, not billions).
- Rejected: Pinecone/Weaviate/etc. — operational surface and a tenancy model to reinvent,
  unjustified at this scale.
