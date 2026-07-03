package org.donorly.backend.repository;

import org.donorly.backend.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByOrganizationIdOrderByIssuedAtDesc(UUID organizationId);
    Optional<Receipt> findByPaymentId(UUID paymentId);
    Optional<Receipt> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}
