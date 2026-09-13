package io.github.dmitrypoverov.registry.registrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.registry.support.IntegrationTest;
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
import org.springframework.transaction.support.TransactionTemplate;

class RegistrationConcurrentInsertTest extends IntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // Гонка подстроена, а не случайна: незакоммиченную вставку теста запрос при поиске не видит,
    // доходит до своей вставки и ждёт на уникальном индексе, пока тест не закоммитит.
    @Test
    void register_whileSameContractInsertIsUncommitted_waitsAndReturnsOkWithCommittedRecord() {
        UUID contractId = UUID.randomUUID();

        CompletableFuture<Void> repeatedRegistration = transactionTemplate.execute(status -> {
            registrationRepository.saveAndFlush(Registration.register(requestFor(contractId), Instant.now()));

            CompletableFuture<Void> request = CompletableFuture.runAsync(
                    () -> register(contractId).expectStatus().isOk());
            assertThatThrownBy(() -> request.get(500, TimeUnit.MILLISECONDS))
                    .as("insert must wait for the uncommitted duplicate")
                    .isInstanceOf(TimeoutException.class);

            return request;
        });

        assertThat(repeatedRegistration).succeedsWithin(Duration.ofSeconds(5));
        assertThat(registrationRepository.findAll()).hasSize(1);
    }

    private static RegistrationRequest requestFor(UUID contractId) {
        return new RegistrationRequest(
                contractId,
                "LI-2026-000042",
                "Ivan Petrov",
                LocalDate.of(1990, 5, 17),
                "AB1234567",
                new BigDecimal("1000000.00"),
                new BigDecimal("65000.00"),
                LocalDate.of(2026, 9, 13),
                LocalDate.of(2036, 9, 12));
    }
}
