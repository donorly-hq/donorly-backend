package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fundraising_campaigns")
public class FundraisingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "default_fund_id")
    private UUID defaultFundId;

    @Column(name = "campaign_code", nullable = false)
    private String campaignCode;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "campaign_description")
    private String campaignDescription;

    // draft, active, paused, completed, archived
    @Column(name = "campaign_status", nullable = false)
    private String campaignStatus = "draft";

    @Column(name = "goal_amount", nullable = false)
    private BigDecimal goalAmount = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "public_slug")
    private String publicSlug;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getDefaultFundId() { return defaultFundId; }
    public void setDefaultFundId(UUID defaultFundId) { this.defaultFundId = defaultFundId; }
    public String getCampaignCode() { return campaignCode; }
    public void setCampaignCode(String campaignCode) { this.campaignCode = campaignCode; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public String getCampaignDescription() { return campaignDescription; }
    public void setCampaignDescription(String campaignDescription) { this.campaignDescription = campaignDescription; }
    public String getCampaignStatus() { return campaignStatus; }
    public void setCampaignStatus(String campaignStatus) { this.campaignStatus = campaignStatus; }
    public BigDecimal getGoalAmount() { return goalAmount; }
    public void setGoalAmount(BigDecimal goalAmount) { this.goalAmount = goalAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getPublicSlug() { return publicSlug; }
    public void setPublicSlug(String publicSlug) { this.publicSlug = publicSlug; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
}
