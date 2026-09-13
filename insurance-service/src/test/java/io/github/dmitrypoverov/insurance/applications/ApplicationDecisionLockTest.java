package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

class ApplicationDecisionLockTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void reject_whileApprovalHoldsLock_waitsAndReturnsConflict() {
        UUID id = applicationRepository.save(submittedApplication()).getId();

        CompletableFuture<Void> rejection = transactionTemplate.execute(status -> {
            Application locked = applicationRepository.findWithLockById(id).orElseThrow();

            CompletableFuture<Void> request = CompletableFuture.runAsync(() -> sendReject(id));
            assertThatThrownBy(() -> request.get(500, TimeUnit.MILLISECONDS))
                    .as("reject must wait for the row lock")
                    .isInstanceOf(TimeoutException.class);

            locked.approve(UNDERWRITER_SUBJECT, Instant.now());
            return request;
        });

        assertThat(rejection).succeedsWithin(Duration.ofSeconds(5));
        assertThat(applicationRepository.findById(id)).get()
                .extracting(Application::getStatus)
                .isEqualTo(ApplicationStatus.APPROVED);
    }

    private void sendReject(UUID id) {
        client.post()
                .uri("/api/v1/applications/{id}/reject", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.tokenFor(UNDERWRITER_SUBJECT, "underwriter"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "reason": "Late rejection" }
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private static Application submittedApplication() {
        return Application.submit(
                CUSTOMER_SUBJECT,
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("65000.00"));
    }
}
