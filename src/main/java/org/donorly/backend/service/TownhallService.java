package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.TownhallRequest;
import org.donorly.backend.model.Townhall;
import org.donorly.backend.repository.TownhallRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TownhallService {

    private final TownhallRepository townhallRepository;
    private final AuditService auditService;

    public List<Townhall> list() {
        return townhallRepository.findByOrganizationIdOrderByEventDateDescEventTimeDesc(
                TenantContext.requireOrganizationId());
    }

    public Townhall get(UUID id) {
        return townhallRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Townhall not found"));
    }

    @Transactional
    public Townhall create(TownhallRequest request) {
        Townhall townhall = new Townhall();
        townhall.setOrganizationId(TenantContext.requireOrganizationId());
        apply(townhall, request);
        Townhall saved = townhallRepository.save(townhall);
        auditService.record("townhall.create", "townhall", saved.getId());
        return saved;
    }

    @Transactional
    public Townhall update(UUID id, TownhallRequest request) {
        Townhall townhall = get(id);
        apply(townhall, request);
        Townhall saved = townhallRepository.save(townhall);
        auditService.record("townhall.update", "townhall", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Townhall townhall = get(id);
        townhallRepository.delete(townhall);
        auditService.record("townhall.delete", "townhall", id);
    }

    private void apply(Townhall townhall, TownhallRequest request) {
        townhall.setPersonName(request.personName());
        townhall.setPhone(request.phone());
        townhall.setVenue(request.venue());
        townhall.setAddress(request.address());
        townhall.setEventDate(request.eventDate());
        townhall.setEventTime(request.eventTime());
        townhall.setDurationMinutes(request.durationMinutes());
        townhall.setHostAmbassadorUserId(request.hostAmbassadorUserId());
        townhall.setExpectedRsvps(request.expectedRsvps());
        townhall.setNotes(request.notes());
    }
}
