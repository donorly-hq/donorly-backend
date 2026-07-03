package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.MessageTemplateRequest;
import org.donorly.backend.model.MessageTemplate;
import org.donorly.backend.service.MessageTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communications/templates")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('communications.read')")
    public List<MessageTemplate> list() {
        return templateService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('communications.read')")
    public MessageTemplate get(@PathVariable UUID id) {
        return templateService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('communications.manage')")
    public ResponseEntity<MessageTemplate> create(@Valid @RequestBody MessageTemplateRequest request) {
        return ResponseEntity.ok(templateService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('communications.manage')")
    public MessageTemplate update(@PathVariable UUID id, @Valid @RequestBody MessageTemplateRequest request) {
        return templateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('communications.manage')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
