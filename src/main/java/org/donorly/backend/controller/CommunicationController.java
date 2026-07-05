package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.BroadcastRequest;
import org.donorly.backend.dto.CommunicationMessageResponse;
import org.donorly.backend.dto.SendMessageRequest;
import org.donorly.backend.dto.SendResultResponse;
import org.donorly.backend.service.CommunicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/communications")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;

    /** Without {@code page}, returns the full list (legacy behavior); with it, a page envelope. */
    @GetMapping("/messages")
    @PreAuthorize("hasAuthority('communications.read')")
    public Object listMessages(@RequestParam(required = false) Integer page,
                               @RequestParam(defaultValue = "50") int size) {
        if (page == null) {
            return communicationService.listMessages();
        }
        return communicationService.pageMessages(page, size);
    }

    @GetMapping("/messages/donor/{donorId}")
    @PreAuthorize("hasAuthority('communications.read')")
    public List<CommunicationMessageResponse> listForDonor(@PathVariable UUID donorId) {
        return communicationService.listMessagesForDonor(donorId);
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('communications.send')")
    public ResponseEntity<CommunicationMessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(communicationService.send(request));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAuthority('communications.send')")
    public ResponseEntity<SendResultResponse> broadcast(@Valid @RequestBody BroadcastRequest request) {
        return ResponseEntity.ok(communicationService.broadcast(request));
    }
}
