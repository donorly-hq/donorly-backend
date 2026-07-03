package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organization_settings")
@Getter
@Setter
public class OrganizationSettings extends AuditableEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "receipt_prefix", length = 20)
    private String receiptPrefix;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "USD";

    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled = false;

    @Column(name = "payment_enabled", nullable = false)
    private boolean paymentEnabled = false;
}
