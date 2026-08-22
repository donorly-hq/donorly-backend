package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * One audience selector attached to a campaign. Exactly one of
 * {@code donorId} (individual), {@code tagId} (group), or {@code state}
 * (geography) is set — enforced by a DB check constraint. The campaign's
 * targeted-donor count is the distinct union of all its selectors.
 */
@Entity
@Table(name = "campaign_targets")
@Getter
@Setter
public class CampaignTarget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "tag_id")
    private UUID tagId;

    private String state;
}
