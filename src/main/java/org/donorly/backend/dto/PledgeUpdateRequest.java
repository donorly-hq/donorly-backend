package org.donorly.backend.dto;

import java.math.BigDecimal;

public record PledgeUpdateRequest(
        BigDecimal amount,
        // collectedAmount is intentionally NOT editable here: the collected total is
        // derived from recorded payments (see PaymentService), which enforce the
        // "cannot exceed pledged" rule, issue receipts, and write audit entries.
        String status,
        String paymentMethod,
        String notes
) {
}
