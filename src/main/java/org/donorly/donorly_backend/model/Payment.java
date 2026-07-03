package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Replaces paymentMethod / collected boolean that used to live on
 * Donor. A pledge can now have multiple payments (partial payments
 * over time), which the old boolean flag couldn't express.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "pledge_id")
    private UUID pledgeId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "fund_id")
    private UUID fundId;

    @Column(name = "payment_number", nullable = false)
    private String paymentNumber;

    // pending, processing, succeeded, failed, refunded, cancelled
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "pending";

    // cash, check, credit_card, ach, zelle, bank_transfer, stock, in_kind, other
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "processing_fee_amount", nullable = false)
    private BigDecimal processingFeeAmount = BigDecimal.ZERO;

    // net_amount is a Postgres GENERATED column (gross - fee) — do not set from Java
    @Column(name = "net_amount", insertable = false, updatable = false)
    private BigDecimal netAmount;

    @Column(name = "payment_received_at")
    private Instant paymentReceivedAt;

    @Column(name = "external_processor")
    private String externalProcessor;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "memo")
    private String memo;

    @Column(name = "received_by_user_id")
    private UUID receivedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPledgeId() { return pledgeId; }
    public void setPledgeId(UUID pledgeId) { this.pledgeId = pledgeId; }
    public UUID getCampaignId() { return campaignId; }
    public void setCampaignId(UUID campaignId) { this.campaignId = campaignId; }
    public UUID getDonorId() { return donorId; }
    public void setDonorId(UUID donorId) { this.donorId = donorId; }
    public UUID getFundId() { return fundId; }
    public void setFundId(UUID fundId) { this.fundId = fundId; }
    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getProcessingFeeAmount() { return processingFeeAmount; }
    public void setProcessingFeeAmount(BigDecimal processingFeeAmount) { this.processingFeeAmount = processingFeeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public Instant getPaymentReceivedAt() { return paymentReceivedAt; }
    public void setPaymentReceivedAt(Instant paymentReceivedAt) { this.paymentReceivedAt = paymentReceivedAt; }
    public String getExternalProcessor() { return externalProcessor; }
    public void setExternalProcessor(String externalProcessor) { this.externalProcessor = externalProcessor; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public UUID getReceivedByUserId() { return receivedByUserId; }
    public void setReceivedByUserId(UUID receivedByUserId) { this.receivedByUserId = receivedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
