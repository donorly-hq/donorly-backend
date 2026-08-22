package org.donorly.backend.repository;

import org.donorly.backend.model.CampaignTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CampaignTargetRepository extends JpaRepository<CampaignTarget, UUID> {

    List<CampaignTarget> findByCampaignIdAndOrganizationId(UUID campaignId, UUID organizationId);

    void deleteByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Resolves every selector (individual donors, tag groups, states) into the
     * distinct set of live donor ids the campaign targets.
     */
    @Query("""
            select distinct d.id from Donor d
            where d.organizationId = :orgId and d.deletedAt is null
              and (
                exists (select 1 from CampaignTarget ct
                        where ct.campaignId = :campaignId and ct.donorId = d.id)
                or exists (select 1 from CampaignTarget ct, DonorTagAssignment dta
                           where ct.campaignId = :campaignId and ct.tagId = dta.tagId
                             and dta.donorId = d.id)
                or exists (select 1 from CampaignTarget ct
                           where ct.campaignId = :campaignId and ct.state is not null
                             and lower(ct.state) = lower(coalesce(d.state, '')))
              )
            """)
    List<UUID> resolveTargetedDonorIds(@Param("campaignId") UUID campaignId, @Param("orgId") UUID orgId);
}
