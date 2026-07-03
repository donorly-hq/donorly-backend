package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Links an AppUser to an AppTenant with org-specific details.
 * This is the tenant-scoped "membership" row. Role assignment lives
 * in TenantUserRole (join to SecurityRole), so one person can have
 * different roles in different organizations.
 */
@Entity
@Table(name = "tenant_users")
public class TenantUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tenant_user_id")
    private UUID tenantUserId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "is_primary_admin", nullable = false)
    private boolean isPrimaryAdmin = false;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt = Instant.now();

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getTenantUserId() { return tenantUserId; }
    public void setTenantUserId(UUID tenantUserId) { this.tenantUserId = tenantUserId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public boolean isPrimaryAdmin() { return isPrimaryAdmin; }
    public void setPrimaryAdmin(boolean primaryAdmin) { isPrimaryAdmin = primaryAdmin; }
    public Instant getInvitedAt() { return invitedAt; }
    public void setInvitedAt(Instant invitedAt) { this.invitedAt = invitedAt; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getRemovedAt() { return removedAt; }
    public void setRemovedAt(Instant removedAt) { this.removedAt = removedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
