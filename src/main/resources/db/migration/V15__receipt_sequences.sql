-- Atomic, gap-free-per-org receipt numbering.
-- Previously receipt numbers came from COUNT(*)+1, which is not atomic: two
-- concurrent payments could read the same count and mint duplicate numbers.
-- This table holds one counter row per (organization, year); the number is
-- allocated with a single INSERT ... ON CONFLICT ... RETURNING statement, which
-- Postgres executes atomically under row locking.

CREATE TABLE receipt_sequences (
    organization_id UUID   NOT NULL REFERENCES organizations(id),
    year            INT    NOT NULL,
    last_seq        BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (organization_id, year)
);

-- Seed each org's current year counter from existing receipts so numbering
-- continues without collisions after deploy. Extract the trailing numeric group
-- of receipt_number (format PREFIX-YYYY-NNNNN); ignore anything non-conforming.
INSERT INTO receipt_sequences (organization_id, year, last_seq)
SELECT r.organization_id,
       EXTRACT(YEAR FROM now())::INT AS year,
       COALESCE(MAX((regexp_replace(r.receipt_number, '^.*-', ''))::BIGINT), 0)
FROM receipts r
WHERE r.receipt_number ~ '-[0-9]+$'
GROUP BY r.organization_id;
