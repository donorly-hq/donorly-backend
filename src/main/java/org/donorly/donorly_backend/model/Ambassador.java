package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-scoped ambassador. Hierarchy fields (parentAmbassadorId,
 * ancestorPath) require the V2__ambassador_hierarchy.sql migration
 * to be run in addition to V1 — they were not part of the original
 * V1 schema and were added back to preserve the Phase 3 handover
 * feature's existing logic.
 */
@Entity
@Table(name = "ambassadors")
public class Ambassador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ambassador_id")
    private UUID ambassadorId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_user_id")
    private UUID tenantUserId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "ambassador_code")
    private String ambassadorCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "parent_ambassador_id")
    private UUID parentAmbassadorId;

    @ElementCollection
    @CollectionTable(name = "ambassador_ancestor_path", joinColumns = @JoinColumn(name = "ambassador_id"))
    @Column(name = "ancestor_id")
    private java.util.List<UUID> ancestorPath;

    public UUID getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(UUID ambassadorId) { this.ambassadorId = ambassadorId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getTenantUserId() { return tenantUserId; }
    public void setTenantUserId(UUID tenantUserId) { this.tenantUserId = tenantUserId; }
    public UUID getDonorId() { return donorId; }
    public void setDonorId(UUID donorId) { this.donorId = donorId; }
    public String getAmbassadorCode() { return ambassadorCode; }
    public void setAmbassadorCode(String ambassadorCode) { this.ambassadorCode = ambassadorCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getParentAmbassadorId() { return parentAmbassadorId; }
    public void setParentAmbassadorId(UUID parentAmbassadorId) { this.parentAmbassadorId = parentAmbassadorId; }
    public java.util.List<UUID> getAncestorPath() { return ancestorPath; }
    public void setAncestorPath(java.util.List<UUID> ancestorPath) { this.ancestorPath = ancestorPath; }
}
