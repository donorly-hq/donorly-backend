package org.donorly.backend;

import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.dto.PaymentRequest;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.service.PaymentService;
import org.donorly.backend.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Financial integrity under concurrency (July 2026 review, High items):
 *
 * 1. collectedAmount is protected by a pessimistic row lock, so N parallel payments
 *    must sum exactly — no lost updates from read-modify-write races.
 * 2. Receipt numbers are allocated from an atomic per-org counter, so parallel
 *    receipt issuance must never mint duplicates.
 */
class FinancialConcurrencyTest extends IntegrationTestBase {

    private static final int THREADS = 8;

    @Autowired private PaymentService paymentService;

    private Organization org;
    private TestActor financeUser;
    private Campaign campaign;
    private Donor donor;

    @BeforeEach
    void setUp() {
        org = createOrg("Finance Org");
        financeUser = createActor(org, "finance_user");
        campaign = createCampaign(org, "Finance Campaign");
        donor = createDonor(org, "Concurrent Donor");
    }

    @Test
    void concurrentPaymentsDoNotLoseUpdates() throws Exception {
        Pledge pledge = createPledge(org, campaign, donor, new BigDecimal("1000"));

        runConcurrently(THREADS, () -> paymentService.record(new PaymentRequest(
                pledge.getId(), new BigDecimal("10"), "cash", null, null, null, false)));

        Pledge reloaded = pledgeRepository.findById(pledge.getId()).orElseThrow();
        assertThat(reloaded.getCollectedAmount())
                .isEqualByComparingTo(new BigDecimal("80")); // 8 threads x 10 — nothing lost
    }

    @Test
    void concurrentReceiptNumbersAreUnique() throws Exception {
        // Separate pledges so the pledge row lock does not mask a counter race.
        List<Pledge> pledges = java.util.stream.IntStream.range(0, THREADS)
                .mapToObj(i -> createPledge(org, campaign, donor, new BigDecimal("100")))
                .toList();

        List<String> receiptNumbers = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (Pledge pledge : pledges) {
                pool.submit(() -> {
                    try {
                        start.await();
                        withTenant(() -> {
                            var response = paymentService.record(new PaymentRequest(
                                    pledge.getId(), new BigDecimal("100"), "cash",
                                    null, null, null, true));
                            receiptNumbers.add(response.receipt().receiptNumber());
                        });
                    } catch (Exception ignored) {
                        // a failed thread shows up as a missing receipt below
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(receiptNumbers).hasSize(THREADS);
        Set<String> unique = receiptNumbers.stream().collect(Collectors.toSet());
        assertThat(unique).as("all receipt numbers distinct: " + receiptNumbers).hasSize(THREADS);
    }

    @Test
    void paymentCannotExceedPledgedAmount() {
        Pledge pledge = createPledge(org, campaign, donor, new BigDecimal("50"));
        assertThrows(BadRequestException.class,
                () -> withTenant(() -> paymentService.record(new PaymentRequest(
                        pledge.getId(), new BigDecimal("60"), "cash", null, null, null, false))));
    }

    /** Runs the action concurrently on N threads, each with the tenant context set. */
    private void runConcurrently(int threads, Runnable action) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        withTenant(action);
                    } catch (Exception ignored) {
                        // assertion on the final sum surfaces any silent failure
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    /** Services read org/user from the thread-local TenantContext; worker threads must set it. */
    private void withTenant(Runnable action) {
        UUID orgId = org.getId();
        UUID userId = financeUser.user().getId();
        TenantContext.setOrganizationId(orgId);
        TenantContext.setUserId(userId);
        try {
            action.run();
        } finally {
            TenantContext.clear();
        }
    }
}
