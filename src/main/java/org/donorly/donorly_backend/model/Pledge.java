package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Replaces pledgeAmount / totalCommitment fields that used to live
 * directly on Donor. A donor can now have multiple pledges over time,
 * across different campaigns — which the old flat model couldn't express.
 */
@Entity
@Table(name = "pledges")
public class Pledge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pledge_id")
    private UUID pledgeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "ambassador_id")
    private UUID ambassadorId;

    @Column(name = "fund_id")
    private UUID fundId;

    @Column(name = "pledge_number", nullable = false)
    private String pledgeNumber;

    // draft, committed, partially_paid, paid, cancelled, written_off
    @Column(name = "pledge_status", nullable = false)
    private String pledgeStatus = "committed";

    @Column(name = "pledged_amount", nullable = false)
    private BigDecimal pledgedAmount;

    // one_time, weekly, monthly, quarterly, annually
    @Column(name = "recurrence_frequency", nullable = false)
    private String recurrenceFrequency = "one_time";

    @Column(name = "pledge_start_date", nullable = false)
    private LocalDate pledgeStartDate = LocalDate.now();

    @Column(name = "pledge_end_date")
    private LocalDate pledgeEndDate;

    @Column(name = "public_note")
    private String publicNote;

    @Column(name = "internal_note")
    private String internalNote;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Portal-compatibility fields (see V4 migration)
    @Column(name = "donation_type")
    private String donationType;

    @Column(name = "is_corporate_match", nullable = false)
    private boolean corporateMatch = false;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "preferred_payment_method")
    private String preferredPaymentMethod;

    public UUID getPledgeId() { return pledgeId; }
    public void setPledgeId(UUID pledgeId) { this.pledgeId = pledgeId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
    public UUID getDonorId() { return donorId; }
    public void setDonorId(UUID donorId) { this.donorId = donorId; }
    public UUID getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(UUID ambassadorId) { this.ambassadorId = ambassadorId; }
    public UUID getFundId() { return fundId; }
    public void setFundId(UUID fundId) { this.fundId = fundId; }
    public String getPledgeNumber() { return pledgeNumber; }
    public void setPledgeNumber(String pledgeNumber) { this.pledgeNumber = pledgeNumber; }
    public String getPledgeStatus() { return pledgeStatus; }
    public void setPledgeStatus(String pledgeStatus) { this.pledgeStatus = pledgeStatus; }
    public BigDecimal getPledgedAmount() { return pledgedAmount; }
    public void setPledgedAmount(BigDecimal pledgedAmount) { this.pledgedAmount = pledgedAmount; }
    public String getRecurrenceFrequency() { return recurrenceFrequency; }
    public void setRecurrenceFrequency(String recurrenceFrequency) { this.recurrenceFrequency = recurrenceFrequency; }
    public LocalDate getPledgeStartDate() { return pledgeStartDate; }
    public void setPledgeStartDate(LocalDate pledgeStartDate) { this.pledgeStartDate = pledgeStartDate; }
    public LocalDate getPledgeEndDate() { return pledgeEndDate; }
    public void setPledgeEndDate(LocalDate pledgeEndDate) { this.pledgeEndDate = pledgeEndDate; }
    public String getPublicNote() { return publicNote; }
    public void setPublicNote(String publicNote) { this.publicNote = publicNote; }
    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getDonationType() { return donationType; }
    public void setDonationType(String donationType) { this.donationType = donationType; }
    public boolean isCorporateMatch() { return corporateMatch; }
    public void setCorporateMatch(boolean corporateMatch) { this.corporateMatch = corporateMatch; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getPreferredPaymentMethod() { return preferredPaymentMethod; }
    public void setPreferredPaymentMethod(String preferredPaymentMethod) { this.preferredPaymentMethod = preferredPaymentMethod; }
}
