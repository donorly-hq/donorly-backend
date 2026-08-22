package org.donorly.backend.repository;

import org.donorly.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    org.springframework.data.domain.Page<Payment> findByOrganizationId(
            UUID organizationId, org.springframework.data.domain.Pageable pageable);
    List<Payment> findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(UUID organizationId, UUID donorId);
    List<Payment> findByOrganizationIdAndPledgeIdOrderByCreatedAtDesc(UUID organizationId, UUID pledgeId);
    Optional<Payment> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
    List<Payment> findTop5ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    /** Webhook idempotency: Stripe session ids are stored in {@code reference}. */
    boolean existsByOrganizationIdAndReference(UUID organizationId, String reference);
}
