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

    @Column(name = "pledge_card_auto_approve", nullable = false)
    private boolean pledgeCardAutoApprove = true;

    @Column(name = "pledge_card_auto_approve_hours", nullable = false)
    private int pledgeCardAutoApproveHours = 24;

    @Column(name = "pledge_card_reminders_enabled", nullable = false)
    private boolean pledgeCardRemindersEnabled = true;

    @Column(name = "pledge_card_reminder_interval_days", nullable = false)
    private int pledgeCardReminderIntervalDays = 3;

    @Column(name = "pledge_card_reminder_max", nullable = false)
    private int pledgeCardReminderMax = 3;
}
