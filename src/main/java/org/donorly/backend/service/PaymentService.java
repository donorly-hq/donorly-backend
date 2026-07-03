package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.PaymentRequest;
import org.donorly.backend.dto.PaymentResponse;
import org.donorly.backend.dto.ReceiptResponse;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.Payment;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.Receipt;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PaymentRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.repository.ReceiptRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final AuditService auditService;

    public List<PaymentResponse> list() {
        return paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(TenantContext.requireOrganizationId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PaymentResponse> listByDonor(UUID donorId) {
        return paymentRepository
                .findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(
                        TenantContext.requireOrganizationId(), donorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse get(UUID id) {
        return toResponse(findPayment(id));
    }

    @Transactional
    public PaymentResponse record(PaymentRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        Pledge pledge = pledgeRepository.findByIdAndOrganizationId(request.pledgeId(), orgId)
                .orElseThrow(() -> new NotFoundException("Pledge not found"));

        BigDecimal newCollected = pledge.getCollectedAmount().add(request.amount());
        if (newCollected.compareTo(pledge.getAmount()) > 0) {
            throw new BadRequestException("Payment would exceed the pledged amount");
        }

        Payment payment = new Payment();
        payment.setOrganizationId(orgId);
        payment.setPledgeId(pledge.getId());
        payment.setDonorId(pledge.getDonorId());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());
        payment.setReference(request.reference());
        payment.setNotes(request.notes());
        payment.setRecordedBy(TenantContext.getUserId());
        payment = paymentRepository.save(payment);

        pledge.setCollectedAmount(newCollected);
        if (newCollected.compareTo(pledge.getAmount()) >= 0) {
            pledge.setStatus("fulfilled");
        } else if ("pending".equals(pledge.getStatus())) {
            pledge.setStatus("active");
        }
        pledgeRepository.save(pledge);

        recomputeDonorLifetimeGiving(orgId, pledge.getDonorId());

        Receipt receipt = null;
        if (request.issueReceipt()) {
            receipt = issueReceipt(payment);
        }

        auditService.record("payment.record", "payment", payment.getId());
        return toResponse(payment, receipt);
    }

    public ReceiptResponse getReceipt(UUID paymentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        findPayment(paymentId);
        return receiptRepository.findByPaymentId(paymentId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .map(this::toReceiptResponse)
                .orElseThrow(() -> new NotFoundException("Receipt not found"));
    }

    @Transactional
    public ReceiptResponse issueReceiptForPayment(UUID paymentId) {
        Payment payment = findPayment(paymentId);
        if (receiptRepository.findByPaymentId(paymentId).isPresent()) {
            throw new BadRequestException("Receipt already issued for this payment");
        }
        Receipt receipt = issueReceipt(payment);
        auditService.record("receipt.issue", "receipt", receipt.getId());
        return toReceiptResponse(receipt);
    }

    private Receipt issueReceipt(Payment payment) {
        UUID orgId = payment.getOrganizationId();
        Donor donor = donorRepository.findByIdAndOrganizationId(payment.getDonorId(), orgId)
                .orElseThrow(() -> new NotFoundException("Donor not found"));

        String prefix = settingsRepository.findById(orgId)
                .map(OrganizationSettings::getReceiptPrefix)
                .filter(p -> p != null && !p.isBlank())
                .orElse("RCP");

        long seq = receiptRepository.countByOrganizationId(orgId) + 1;
        String receiptNumber = "%s-%d-%05d".formatted(prefix, Year.now().getValue(), seq);

        Receipt receipt = new Receipt();
        receipt.setOrganizationId(orgId);
        receipt.setPaymentId(payment.getId());
        receipt.setReceiptNumber(receiptNumber);
        receipt.setIssuedTo(donor.getFullName());
        receipt.setAmount(payment.getAmount());
        receipt.setIssuedBy(TenantContext.getUserId());
        return receiptRepository.save(receipt);
    }

    private void recomputeDonorLifetimeGiving(UUID orgId, UUID donorId) {
        BigDecimal collected = pledgeRepository.findByOrganizationIdAndDonorId(orgId, donorId).stream()
                .map(Pledge::getCollectedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        donorRepository.findByIdAndOrganizationId(donorId, orgId).ifPresent(donor -> {
            donor.setLifetimeGiving(collected);
            donorRepository.save(donor);
        });
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        Receipt receipt = receiptRepository.findByPaymentId(payment.getId()).orElse(null);
        return toResponse(payment, receipt);
    }

    private PaymentResponse toResponse(Payment payment, Receipt receipt) {
        String donorName = donorRepository.findById(payment.getDonorId())
                .map(Donor::getFullName)
                .orElse(null);
        return new PaymentResponse(
                payment.getId(),
                payment.getPledgeId(),
                payment.getDonorId(),
                donorName,
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentDate(),
                payment.getReference(),
                payment.getNotes(),
                payment.getRecordedBy(),
                payment.getCreatedAt(),
                receipt != null ? toReceiptResponse(receipt) : null
        );
    }

    private ReceiptResponse toReceiptResponse(Receipt receipt) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getPaymentId(),
                receipt.getReceiptNumber(),
                receipt.getIssuedTo(),
                receipt.getAmount(),
                receipt.getIssuedAt()
        );
    }
}
