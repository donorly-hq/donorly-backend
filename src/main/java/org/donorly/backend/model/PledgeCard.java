package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pledge_cards")
@Getter
@Setter
public class PledgeCard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "image_url")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_json", columnDefinition = "jsonb")
    private String extractedJson;

    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String notes;

    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = "pending";
}
