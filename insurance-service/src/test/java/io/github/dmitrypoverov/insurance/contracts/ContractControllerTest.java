package io.github.dmitrypoverov.insurance.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.applications.ApplicationStatus;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;

class ContractControllerTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";
    private static final String CONTRACT_NUMBER_FORMAT = "LI-\\d{4}-\\d{6}";

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void issue_approvedApplication_createsContractAndPendingRegistration() {
        UUID applicationId = saveApprovedApplication();

        issueContract(applicationId)
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .jsonPath("$.applicationId").isEqualTo(applicationId.toString())
                .jsonPath("$.policyholderSubject").isEqualTo(CUSTOMER_SUBJECT)
                .jsonPath("$.contractNumber")
                .value(number -> assertThat(number.toString()).matches(CONTRACT_NUMBER_FORMAT));

        Application application = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CONTRACT_ISSUED);

        List<Contract> contracts = contractRepository.findAll();
        assertThat(contracts).hasSize(1);

        List<ContractRegistration> registrations = contractRegistrationRepository.findAll();
        assertThat(registrations).hasSize(1);
        assertThat(registrations.getFirst().getContractId()).isEqualTo(contracts.getFirst().getId());
        assertThat(registrations.getFirst().getStatus()).isEqualTo(RegistrationStatus.PENDING);
    }

    @Test
    void issue_repeatedRequest_returnsOkWithSameContract() {
        UUID applicationId = saveApprovedApplication();
        issueContract(applicationId).expectStatus().isCreated();

        issueContract(applicationId)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationId").isEqualTo(applicationId.toString());

        assertThat(contractRepository.findAll()).hasSize(1);
        assertThat(contractRegistrationRepository.findAll()).hasSize(1);
    }

    @Test
    void issue_submittedApplication_returnsConflict() {
        UUID applicationId = applicationRepository.save(submittedApplication()).getId();

        issueContract(applicationId)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");

        assertThat(contractRepository.findAll()).isEmpty();
    }

    @Test
    void issue_nonExistentApplication_returnsNotFound() {
        issueContract(UUID.randomUUID())
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void issue_withCustomerToken_returnsForbidden() {
        UUID applicationId = saveApprovedApplication();

        client.post()
                .uri("/api/v1/applications/{id}/contract", applicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void issue_whileApplicationLockedLongerThanTimeout_returnsServiceUnavailable() {
        UUID applicationId = saveApprovedApplication();

        transactionTemplate.executeWithoutResult(status -> {
            applicationRepository.findWithLockById(applicationId).orElseThrow();

            CompletableFuture<Void> request = CompletableFuture.runAsync(() -> expectServiceBusy(applicationId));

            assertThat(request).succeedsWithin(10, TimeUnit.SECONDS);
        });

        assertThat(contractRepository.findAll()).isEmpty();
    }

    private void expectServiceBusy(UUID applicationId) {
        issueContract(applicationId)
                .expectStatus().isEqualTo(503)
                .expectHeader().exists(HttpHeaders.RETRY_AFTER)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_BUSY");
    }

    private RestTestClient.ResponseSpec issueContract(UUID applicationId) {
        return client.post()
                .uri("/api/v1/applications/{id}/contract", applicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange();
    }

    private UUID saveApprovedApplication() {
        Application application = submittedApplication();
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        return applicationRepository.save(application).getId();
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

    private String bearer(String subject, String... roles) {
        return "Bearer " + TestJwtTokens.tokenFor(subject, roles);
    }
}
