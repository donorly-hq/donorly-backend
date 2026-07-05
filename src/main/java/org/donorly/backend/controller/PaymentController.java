package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PaymentRequest;
import org.donorly.backend.dto.PaymentResponse;
import org.donorly.backend.dto.ReceiptResponse;
import org.donorly.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Without {@code page}, returns the full list (legacy behavior); with it, a page envelope. */
    @GetMapping
    @PreAuthorize("hasAuthority('payments.manage')")
    public Object list(@RequestParam(required = false) Integer page,
                       @RequestParam(defaultValue = "50") int size) {
        if (page == null) {
            return paymentService.list();
        }
        return paymentService.page(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('payments.manage')")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('payments.manage')")
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.record(request));
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAuthority('receipts.issue')")
    public ReceiptResponse getReceipt(@PathVariable UUID paymentId) {
        return paymentService.getReceipt(paymentId);
    }

    @PostMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAuthority('receipts.issue')")
    public ResponseEntity<ReceiptResponse> issueReceipt(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.issueReceiptForPayment(paymentId));
    }
}
