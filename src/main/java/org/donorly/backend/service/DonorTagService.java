package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorTagRequest;
import org.donorly.backend.dto.DonorTagResponse;
import org.donorly.backend.model.DonorTag;
import org.donorly.backend.model.DonorTagAssignment;
import org.donorly.backend.repository.DonorTagAssignmentRepository;
import org.donorly.backend.repository.DonorTagRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonorTagService {

    private final DonorTagRepository tagRepository;
    private final DonorTagAssignmentRepository assignmentRepository;
    private final DonorService donorService;
    private final AuditService auditService;

    public List<DonorTagResponse> listTags() {
        return tagRepository.findByOrganizationIdOrderByName(TenantContext.requireOrganizationId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DonorTagResponse createTag(DonorTagRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (tagRepository.findByOrganizationIdAndNameIgnoreCase(orgId, request.name()).isPresent()) {
            throw new ConflictException("A tag with this name already exists");
        }
        DonorTag tag = new DonorTag();
        tag.setOrganizationId(orgId);
        tag.setName(request.name().trim());
        tag.setColor(request.color());
        DonorTag saved = tagRepository.save(tag);
        auditService.record("donor_tag.create", "donor_tag", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteTag(UUID tagId) {
        UUID orgId = TenantContext.requireOrganizationId();
        DonorTag tag = tagRepository.findByIdAndOrganizationId(tagId, orgId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));
        tagRepository.delete(tag);
        auditService.record("donor_tag.delete", "donor_tag", tagId);
    }

    public List<DonorTagResponse> listDonorTags(UUID donorId) {
        donorService.get(donorId);
        return assignmentRepository.findByDonorId(donorId).stream()
                .map(a -> tagRepository.findById(a.getTagId()).orElse(null))
                .filter(t -> t != null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void assignTag(UUID donorId, UUID tagId) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);
        tagRepository.findByIdAndOrganizationId(tagId, orgId)
                .orElseThrow(() -> new NotFoundException("Tag not found"));
        if (assignmentRepository.existsByDonorIdAndTagId(donorId, tagId)) {
            return;
        }
        DonorTagAssignment assignment = new DonorTagAssignment();
        assignment.setDonorId(donorId);
        assignment.setTagId(tagId);
        assignmentRepository.save(assignment);
        auditService.record("donor_tag.assign", "donor", donorId);
    }

    @Transactional
    public void unassignTag(UUID donorId, UUID tagId) {
        donorService.get(donorId);
        assignmentRepository.deleteByDonorIdAndTagId(donorId, tagId);
        auditService.record("donor_tag.unassign", "donor", donorId);
    }

    private DonorTagResponse toResponse(DonorTag tag) {
        return new DonorTagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
