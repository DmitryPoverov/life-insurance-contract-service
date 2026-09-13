package io.github.dmitrypoverov.insurance.registrations;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.contracts.Contract;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class ContractRegistrationClaimerTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ContractRegistrationClaimer claimer;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void claimNext_rowLockedByAnotherTransaction_skipsToNextPendingTask() {
        Contract lockedContract = saveIssuedContract();
        Contract freeContract = saveIssuedContract();
        makeDueAt(lockedContract.getId(), Instant.now().minusSeconds(5));

        transactionTemplate.executeWithoutResult(status -> {
            contractRegistrationRepository.findWithLockByContractId(lockedContract.getId()).orElseThrow();

            CompletableFuture<Optional<ContractRegistration>> attempt =
                    CompletableFuture.supplyAsync(claimer::claimNext);

            assertThat(attempt).succeedsWithin(Duration.ofSeconds(2));
            assertThat(attempt.join()).get()
                    .extracting(ContractRegistration::getContractId)
                    .isEqualTo(freeContract.getId());
        });
    }

    private void makeDueAt(UUID contractId, Instant when) {
        ContractRegistration registration = contractRegistrationRepository.findByContractId(contractId).orElseThrow();
        registration.scheduleRetry("test setup", when);
        contractRegistrationRepository.save(registration);
    }

    private Contract saveIssuedContract() {
        Application application = Application.submit(
                CUSTOMER_SUBJECT,
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("65000.00"));
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        application.issueContract();
        applicationRepository.save(application);

        Instant now = Instant.now();
        Contract contract = contractRepository.save(
                Contract.issue(application, UNDERWRITER_SUBJECT, now, LocalDate.now(ZoneOffset.UTC)));
        contractRegistrationRepository.save(ContractRegistration.pending(contract.getId(), now, null));
        return contract;
    }
}
