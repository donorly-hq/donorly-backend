package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.BroadcastRequest;
import org.donorly.backend.dto.CommunicationMessageResponse;
import org.donorly.backend.dto.SendMessageRequest;
import org.donorly.backend.dto.SendResultResponse;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.MessageTemplate;
import org.donorly.backend.model.Organization;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.MessageTemplateRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final CommunicationMessageRepository messageRepository;
    private final MessageTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final DonorService donorService;
    private final MessageDeliveryService deliveryService;
    private final TemplateRenderer templateRenderer;
    private final AuditService auditService;

    public List<CommunicationMessageResponse> listMessages() {
        UUID orgId = TenantContext.requireOrganizationId();
        return messageRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CommunicationMessageResponse> listMessagesForDonor(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);
        return messageRepository.findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(orgId, donorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommunicationMessageResponse send(SendMessageRequest request) {
        Donor donor = donorService.get(request.donorId());
        ResolvedMessage resolved = resolveMessage(request.channel(), request.templateId(),
                request.subject(), request.body(), donor);
        return deliverToDonor(donor, request.channel(), resolved, request.templateId());
    }

    @Transactional
    public SendResultResponse broadcast(BroadcastRequest request) {
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (UUID donorId : request.donorIds()) {
            try {
                Donor donor = donorService.get(donorId);
                ResolvedMessage resolved = resolveMessage(request.channel(), request.templateId(),
                        request.subject(), request.body(), donor);
                CommunicationMessageResponse result = deliverToDonor(donor, request.channel(), resolved,
                        request.templateId());
                if ("sent".equals(result.status())) {
                    sent++;
                } else if ("skipped".equals(result.status())) {
                    skipped++;
                } else {
                    failed++;
                }
            } catch (NotFoundException e) {
                skipped++;
            } catch (Exception e) {
                failed++;
            }
        }
        auditService.record("communication.broadcast", "communication", null);
        return new SendResultResponse(sent, skipped, failed);
    }

    private CommunicationMessageResponse deliverToDonor(Donor donor, String channel,
                                                        ResolvedMessage resolved, UUID templateId) {
        String recipient = recipientFor(donor, channel);
        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(TenantContext.requireOrganizationId());
        message.setChannel(channel);
        message.setRecipient(recipient != null ? recipient : "");
        message.setDonorId(donor.getId());
        message.setTemplateId(templateId);
        message.setSubject(resolved.subject());
        message.setBody(resolved.body());
        message.setSentBy(TenantContext.getUserId());

        if (recipient == null || recipient.isBlank()) {
            message.setStatus("skipped");
            message.setErrorMessage(channel.equals("email") ? "Donor has no email" : "Donor has no phone");
            CommunicationMessage saved = messageRepository.save(message);
            return toResponse(saved);
        }

        message.setStatus("queued");
        CommunicationMessage saved = messageRepository.save(message);

        MessageDeliveryService.DeliveryResult result = deliveryService.deliver(
                channel, recipient, resolved.subject(), resolved.body());
        if (result.success()) {
            saved.setStatus("sent");
            saved.setSentAt(Instant.now());
        } else {
            saved.setStatus("failed");
            saved.setErrorMessage(result.errorMessage());
        }
        saved = messageRepository.save(saved);
        auditService.record("communication.send", "communication_message", saved.getId());
        return toResponse(saved);
    }

    private ResolvedMessage resolveMessage(String channel, UUID templateId, String subject, String body,
                                           Donor donor) {
        validateChannel(channel);
        Map<String, String> vars = variablesFor(donor);

        if (templateId != null) {
            MessageTemplate template = templateRepository
                    .findByIdAndOrganizationId(templateId, TenantContext.requireOrganizationId())
                    .orElseThrow(() -> new NotFoundException("Template not found"));
            if (!template.getChannel().equals(channel)) {
                throw new BadRequestException("Template channel does not match send channel");
            }
            String renderedSubject = templateRenderer.render(template.getSubject(), vars);
            String renderedBody = templateRenderer.render(template.getBody(), vars);
            return new ResolvedMessage(renderedSubject, renderedBody);
        }

        if (body == null || body.isBlank()) {
            throw new BadRequestException("Message body is required when no template is selected");
        }
        if ("email".equals(channel) && (subject == null || subject.isBlank())) {
            throw new BadRequestException("Subject is required for email");
        }
        return new ResolvedMessage(
                templateRenderer.render(subject, vars),
                templateRenderer.render(body, vars));
    }

    private Map<String, String> variablesFor(Donor donor) {
        Map<String, String> vars = new HashMap<>();
        vars.put("donor_name", donor.getFullName());
        Organization org = organizationRepository.findById(TenantContext.requireOrganizationId()).orElse(null);
        vars.put("organization_name", org != null ? org.getName() : "");
        return vars;
    }

    private String recipientFor(Donor donor, String channel) {
        if ("email".equals(channel)) {
            return donor.getEmail();
        }
        if ("sms".equals(channel)) {
            return donor.getPhone();
        }
        return null;
    }

    private void validateChannel(String channel) {
        if (!"email".equals(channel) && !"sms".equals(channel)) {
            throw new BadRequestException("Channel must be email or sms");
        }
    }

    private CommunicationMessageResponse toResponse(CommunicationMessage message) {
        String donorName = null;
        if (message.getDonorId() != null) {
            try {
                donorName = donorService.get(message.getDonorId()).getFullName();
            } catch (NotFoundException ignored) {
                // donor may have been removed
            }
        }
        String templateName = null;
        if (message.getTemplateId() != null) {
            templateName = templateRepository.findById(message.getTemplateId())
                    .map(MessageTemplate::getName)
                    .orElse(null);
        }
        return new CommunicationMessageResponse(
                message.getId(),
                message.getChannel(),
                message.getRecipient(),
                message.getDonorId(),
                donorName,
                message.getTemplateId(),
                templateName,
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getErrorMessage(),
                message.getSentAt(),
                message.getCreatedAt()
        );
    }

    private record ResolvedMessage(String subject, String body) {
    }
}
