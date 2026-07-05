package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consolidated annual giving statements — one entry per donor with payments that year. */
public record YearEndStatementResponse(
        int year,
        List<DonorStatement> statements
) {
    public record DonorStatement(
            UUID donorId,
            String donorName,
            String email,
            String city,
            BigDecimal totalGiven,
            List<PaymentLine> payments
    ) {
    }

    public record PaymentLine(
            LocalDate date,
            BigDecimal amount,
            String method,
            String receiptNumber
    ) {
    }
}
