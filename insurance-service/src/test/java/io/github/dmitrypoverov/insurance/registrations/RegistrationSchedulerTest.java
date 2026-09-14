package io.github.dmitrypoverov.insurance.registrations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.contracts.Contract;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.RegistryStubs;
import io.github.dmitrypoverov.insurance.web.CorrelationIdFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RegistrationSchedulerTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private RegistrationScheduler registrationScheduler;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Test
    void processDueRegistrations_registrySucceeds_marksRegistered() {
        Contract contract = saveIssuedContract();
        RegistryStubs.respondWithRegistration(registry, 201);

        registrationScheduler.processDueRegistrations();

        ContractRegistration registration = reload(contract.getId());
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(registration.getRegistryRecordId()).isEqualTo(RegistryStubs.REGISTRY_RECORD_ID);
    }

    @Test
    void processDueRegistrations_registryUnavailableThenRecovers_eventuallyRegisters() {
        Contract contract = saveIssuedContract();
        RegistryStubs.respond(registry, aResponse().withStatus(503));

        registrationScheduler.processDueRegistrations();

        ContractRegistration afterFailure = reload(contract.getId());
        assertThat(afterFailure.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(afterFailure.getAttempts()).isEqualTo(1);
        assertThat(afterFailure.getNextAttemptAt()).isAfter(Instant.now());

        afterFailure.scheduleRetry("forced retry for test", Instant.now());
        contractRegistrationRepository.save(afterFailure);
        RegistryStubs.respondWithRegistration(registry, 201);

        registrationScheduler.processDueRegistrations();

        assertThat(reload(contract.getId()).getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
    }

    @Test
    void processDueRegistrations_taskHasRequestId_propagatesToRegistryRequestHeader() {
        String requestId = "sched-test-" + UUID.randomUUID();
        saveIssuedContract(requestId);
        RegistryStubs.respondWithRegistration(registry, 201);

        registrationScheduler.processDueRegistrations();

        registry.verify(postRequestedFor(urlEqualTo("/api/v1/registrations"))
                .withHeader(CorrelationIdFilter.REQUEST_ID_HEADER, equalTo(requestId)));
    }

    @Test
    void processDueRegistrations_multiplePendingTasks_processesAllInOneTick() {
        Contract first = saveIssuedContract();
        Contract second = saveIssuedContract();
        Contract third = saveIssuedContract();
        RegistryStubs.respondWithRegistration(registry, 201);

        registrationScheduler.processDueRegistrations();

        assertThat(reload(first.getId()).getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(reload(second.getId()).getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(reload(third.getId()).getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
    }

    private ContractRegistration reload(UUID contractId) {
        return contractRegistrationRepository.findByContractId(contractId).orElseThrow();
    }

    private Contract saveIssuedContract() {
        return saveIssuedContract(null);
    }

    private Contract saveIssuedContract(@Nullable String requestId) {
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
        contractRegistrationRepository.save(ContractRegistration.pending(contract.getId(), now, requestId));
        return contract;
    }
}
