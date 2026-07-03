package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Donor identity/contact info only. Pledge amount, payment method,
 * and collection status used to live directly on this entity — they
 * now live on Pledge and Payment, which reference this donor.
 */
@Entity
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "household_id")
    private UUID householdId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "preferred_name")
    private String preferredName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    // individual, business, foundation, family, other
    @Column(name = "donor_type", nullable = false)
    private String donorType = "individual";

    // email, sms, whatsapp, phone, in_person, mail, other
    @Column(name = "communication_preference")
    private String communicationPreference;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous = false;

    @Column(name = "is_major_donor", nullable = false)
    private boolean isMajorDonor = false;

    @Column(name = "donor_notes_private")
    private String donorNotesPrivate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    public UUID getDonorId() { return donorId; }
    public void setDonorId(UUID donorId) { this.donorId = donorId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getHouseholdId() { return householdId; }
    public void setHouseholdId(UUID householdId) { this.householdId = householdId; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getPreferredName() { return preferredName; }
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public String getDonorType() { return donorType; }
    public void setDonorType(String donorType) { this.donorType = donorType; }
    public String getCommunicationPreference() { return communicationPreference; }
    public void setCommunicationPreference(String communicationPreference) { this.communicationPreference = communicationPreference; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public boolean isMajorDonor() { return isMajorDonor; }
    public void setMajorDonor(boolean majorDonor) { isMajorDonor = majorDonor; }
    public String getDonorNotesPrivate() { return donorNotesPrivate; }
    public void setDonorNotesPrivate(String donorNotesPrivate) { this.donorNotesPrivate = donorNotesPrivate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
}
