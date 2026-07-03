package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.MessageTemplateRequest;
import org.donorly.backend.model.MessageTemplate;
import org.donorly.backend.repository.MessageTemplateRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageTemplateService {

    private final MessageTemplateRepository templateRepository;
    private final AuditService auditService;

    public List<MessageTemplate> list() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultTemplates(orgId);
        return templateRepository.findByOrganizationIdOrderByNameAsc(orgId);
    }

    public MessageTemplate get(UUID id) {
        return templateRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Template not found"));
    }

    @Transactional
    public MessageTemplate create(MessageTemplateRequest request) {
        validateChannel(request.channel());
        MessageTemplate template = new MessageTemplate();
        template.setOrganizationId(TenantContext.requireOrganizationId());
        apply(template, request);
        MessageTemplate saved = templateRepository.save(template);
        auditService.record("communication.template.create", "message_template", saved.getId());
        return saved;
    }

    @Transactional
    public MessageTemplate update(UUID id, MessageTemplateRequest request) {
        MessageTemplate template = get(id);
        if (template.isSystem()) {
            throw new BadRequestException("System templates cannot be edited");
        }
        validateChannel(request.channel());
        apply(template, request);
        MessageTemplate saved = templateRepository.save(template);
        auditService.record("communication.template.update", "message_template", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        MessageTemplate template = get(id);
        if (template.isSystem()) {
            throw new BadRequestException("System templates cannot be deleted");
        }
        templateRepository.delete(template);
        auditService.record("communication.template.delete", "message_template", id);
    }

    @Transactional
    protected void ensureDefaultTemplates(UUID orgId) {
        if (templateRepository.countByOrganizationId(orgId) > 0) {
            return;
        }
        saveSystemTemplate(orgId, "Thank you for your pledge", "email",
                "Thank you, {{donor_name}}!",
                "Dear {{donor_name}},\n\nThank you for your generous pledge to {{organization_name}}. "
                        + "We truly appreciate your support.\n\nWarm regards,\n{{organization_name}}");
        saveSystemTemplate(orgId, "Event reminder", "email",
                "Reminder: upcoming event at {{organization_name}}",
                "Hi {{donor_name}},\n\nThis is a friendly reminder about our upcoming event. "
                        + "We hope to see you there!\n\n{{organization_name}}");
        saveSystemTemplate(orgId, "Follow-up SMS", "sms", null,
                "Hi {{donor_name}}, {{organization_name}} here — we'd love to connect about your pledge. "
                        + "Reply or call us when you have a moment.");
    }

    private void saveSystemTemplate(UUID orgId, String name, String channel, String subject, String body) {
        MessageTemplate t = new MessageTemplate();
        t.setOrganizationId(orgId);
        t.setName(name);
        t.setChannel(channel);
        t.setSubject(subject);
        t.setBody(body);
        t.setSystem(true);
        templateRepository.save(t);
    }

    private void apply(MessageTemplate template, MessageTemplateRequest request) {
        template.setName(request.name());
        template.setChannel(request.channel());
        template.setSubject(request.subject());
        template.setBody(request.body());
    }

    private void validateChannel(String channel) {
        if (!"email".equals(channel) && !"sms".equals(channel)) {
            throw new BadRequestException("Channel must be email or sms");
        }
    }
}
