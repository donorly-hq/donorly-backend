package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.AuditLog;
import org.donorly.backend.repository.AuditLogRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(String action, String entityType, UUID entityId) {
        AuditLog log = new AuditLog();
        log.setOrganizationId(TenantContext.getOrganizationId());
        log.setUserId(TenantContext.getUserId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }
}
