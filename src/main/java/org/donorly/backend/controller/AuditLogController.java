package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.AuditLogResponse;
import org.donorly.backend.dto.PageResponse;
import org.donorly.backend.model.AuditLog;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.AuditLogRepository;
import org.donorly.backend.common.PaginationHelper;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('org.settings.manage')")
    public PageResponse<AuditLogResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        UUID orgId = TenantContext.requireOrganizationId();
        var pageable = PaginationHelper.newestFirst(page, size);
        var logs = auditLogRepository.findByOrganizationId(orgId, pageable);

        Map<UUID, User> actors = userRepository.findAllById(
                        logs.getContent().stream()
                                .map(AuditLog::getUserId)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return PageResponse.from(logs).map(log -> {
            User actor = log.getUserId() != null ? actors.get(log.getUserId()) : null;
            return new AuditLogResponse(
                    log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(),
                    log.getUserId(),
                    actor != null ? actor.getFullName() : null,
                    actor != null ? actor.getEmail() : null,
                    log.getCreatedAt());
        });
    }
}
