package org.donorly.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

/**
 * Allocates monotonically increasing receipt numbers per organization and year.
 *
 * The allocation is a single {@code INSERT ... ON CONFLICT ... RETURNING} statement,
 * which Postgres runs atomically (the conflicting row is locked for the update), so
 * two concurrent payments can never receive the same sequence value.
 */
@Service
public class ReceiptNumberService {

    @PersistenceContext
    private EntityManager entityManager;

    /** Returns the next sequence number for the org in the given year (1-based). */
    @Transactional
    public long nextSequence(UUID organizationId, int year) {
        Object seq = entityManager.createNativeQuery("""
                        INSERT INTO receipt_sequences (organization_id, year, last_seq)
                        VALUES (:orgId, :year, 1)
                        ON CONFLICT (organization_id, year)
                        DO UPDATE SET last_seq = receipt_sequences.last_seq + 1
                        RETURNING last_seq
                        """)
                .setParameter("orgId", organizationId)
                .setParameter("year", year)
                .getSingleResult();
        return ((Number) seq).longValue();
    }

    /** Convenience: formats a receipt number like {@code RCP-2026-00042}. */
    @Transactional
    public String nextReceiptNumber(UUID organizationId, String prefix) {
        int year = Year.now().getValue();
        long seq = nextSequence(organizationId, year);
        return "%s-%d-%05d".formatted(prefix, year, seq);
    }
}
