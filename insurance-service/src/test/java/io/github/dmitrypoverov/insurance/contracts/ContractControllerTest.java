package io.github.dmitrypoverov.insurance.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.applications.ApplicationStatus;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import io.github.dmitrypoverov.insurance.registrations.RegistryRecord;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import io.github.dmitrypoverov.insurance.web.CorrelationIdFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private static final String OTHER_CUSTOMER_SUBJECT = "22222222-2222-2222-2222-222222222222";
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
                .jsonPath("$.registration.status").isEqualTo("PENDING")
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
    void issue_withRequestIdHeader_propagatesToResponseAndRegistration() {
        UUID applicationId = saveApprovedApplication();
        String requestId = "test-" + UUID.randomUUID();

        client.post()
                .uri("/api/v1/applications/{id}/contract", applicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .header(CorrelationIdFilter.REQUEST_ID_HEADER, requestId)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(CorrelationIdFilter.REQUEST_ID_HEADER, requestId);

        ContractRegistration registration = contractRegistrationRepository.findAll().getFirst();
        assertThat(registration.getRequestId()).isEqualTo(requestId);
    }

    @Test
    void issue_withoutRequestIdHeader_generatesOne() {
        UUID applicationId = saveApprovedApplication();

        issueContract(applicationId)
                .expectStatus().isCreated()
                .expectHeader().exists(CorrelationIdFilter.REQUEST_ID_HEADER);

        ContractRegistration registration = contractRegistrationRepository.findAll().getFirst();
        assertThat(registration.getRequestId()).isNotNull();
    }

    @Test
    void issue_repeatedRequest_returnsOkWithSameContract() {
        UUID applicationId = saveApprovedApplication();
        String issued = issueContract(applicationId)
                .expectStatus().isCreated()
                .returnResult(String.class)
                .getResponseBody();

        issueContract(applicationId)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationId").isEqualTo(applicationId.toString())
                .jsonPath("$.contractNumber").isEqualTo(JsonPath.read(issued, "$.contractNumber"))
                .jsonPath("$.issuedAt").isEqualTo(JsonPath.read(issued, "$.issuedAt"));

        assertThat(contractRepository.findAll()).hasSize(1);
        assertThat(contractRegistrationRepository.findAll()).hasSize(1);
    }

    @Test
    void issue_submittedApplication_returnsConflict() {
        UUID applicationId = applicationRepository.save(submittedApplication(CUSTOMER_SUBJECT)).getId();

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

    @Test
    void getById_ownContract_returnsOkWithRegistration() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts/{id}", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.contractId").isEqualTo(contract.getId().toString())
                .jsonPath("$.registration.status").isEqualTo("PENDING")
                .jsonPath("$.registration.attempts").isEqualTo(0);
    }

    @Test
    void getById_otherCustomersContract_returnsNotFound() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts/{id}", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void getById_underwriter_returnsOk() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts/{id}", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.contractId").isEqualTo(contract.getId().toString());
    }

    @Test
    void getById_registeredContract_hasNoNextAttemptAt() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);
        ContractRegistration registration =
                contractRegistrationRepository.findByContractId(contract.getId()).orElseThrow();
        registration.markRegistered(new RegistryRecord("GSR-TEST-000001", Instant.now()));
        contractRegistrationRepository.save(registration);

        client.get()
                .uri("/api/v1/contracts/{id}", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.registration.status").isEqualTo("REGISTERED")
                .jsonPath("$.registration.nextAttemptAt").doesNotExist();
    }

    @Test
    void retryRegistration_failedRegistration_returnsPendingAndKeepsAttempts() {
        Contract contract = saveFailedContract();

        client.post()
                .uri("/api/v1/contracts/{id}/registration/retry", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.registration.status").isEqualTo("PENDING")
                .jsonPath("$.registration.attempts").isEqualTo(1);

        ContractRegistration registration = contractRegistrationRepository.findByContractId(contract.getId()).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(registration.getAttempts()).isEqualTo(1);
    }

    @Test
    void retryRegistration_pendingRegistration_returnsConflict() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);

        client.post()
                .uri("/api/v1/contracts/{id}/registration/retry", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");

        ContractRegistration registration = contractRegistrationRepository.findByContractId(contract.getId()).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
    }

    @Test
    void retryRegistration_customerToken_returnsForbidden() {
        Contract contract = saveFailedContract();

        client.post()
                .uri("/api/v1/contracts/{id}/registration/retry", contract.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void retryRegistration_nonExistentContract_returnsNotFound() {
        client.post()
                .uri("/api/v1/contracts/{id}/registration/retry", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void list_customer_returnsOnlyOwnContracts() {
        saveIssuedContract(CUSTOMER_SUBJECT);
        saveIssuedContract(OTHER_CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].policyholderSubject").isEqualTo(CUSTOMER_SUBJECT)
                .jsonPath("$.content[0].registration.status").isEqualTo("PENDING");
    }

    @Test
    void list_withRegistrationStatusFilter_returnsMatchingOnly() {
        saveIssuedContract(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts?registrationStatus=PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1);

        client.get()
                .uri("/api/v1/contracts?registrationStatus=REGISTERED")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void list_withContractNumberFilter_returnsMatchingOnly() {
        Contract wanted = saveIssuedContract(CUSTOMER_SUBJECT);
        saveIssuedContract(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/contracts?contractNumber={number}", wanted.getContractNumber())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].contractId").isEqualTo(wanted.getId().toString());
    }

    @Test
    void list_unsupportedSortProperty_returnsBadRequest() {
        client.get()
                .uri("/api/v1/contracts?sort=premium")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_SORT");
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

    private Contract saveIssuedContract(String policyholderSubject) {
        Application application = submittedApplication(policyholderSubject);
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        application.issueContract();
        applicationRepository.save(application);

        Instant now = Instant.now();
        Contract contract = contractRepository.save(
                Contract.issue(application, UNDERWRITER_SUBJECT, now, LocalDate.now(ZoneOffset.UTC)));
        contractRegistrationRepository.save(ContractRegistration.pending(contract.getId(), now, null));
        return contract;
    }

    private Contract saveFailedContract() {
        Contract contract = saveIssuedContract(CUSTOMER_SUBJECT);
        ContractRegistration registration =
                contractRegistrationRepository.findByContractId(contract.getId()).orElseThrow();
        registration.startAttempt(Instant.now().plusSeconds(30));
        registration.markFailed("boom");
        contractRegistrationRepository.save(registration);
        return contract;
    }

    private UUID saveApprovedApplication() {
        Application application = submittedApplication(CUSTOMER_SUBJECT);
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        return applicationRepository.save(application).getId();
    }

    private static Application submittedApplication(String applicantSubject) {
        return Application.submit(
                applicantSubject,
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
