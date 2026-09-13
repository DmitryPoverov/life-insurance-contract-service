package io.github.dmitrypoverov.insurance.registrations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.contracts.Contract;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.RegistryStubs;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractRegistrationSenderTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ContractRegistrationSender contractRegistrationSender;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Test
    void send_registryCreatesRecord_marksRegisteredAndSendsContract() {
        Contract contract = saveIssuedContract();
        ContractRegistration claimed = claimRegistrationOf(contract.getId());
        RegistryStubs.respondWithRegistration(registry, 201);

        contractRegistrationSender.send(claimed);

        ContractRegistration registration = reload(claimed);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(registration.getRegistryRecordId()).isEqualTo(RegistryStubs.REGISTRY_RECORD_ID);
        assertThat(registration.getRegisteredAt()).isEqualTo(Instant.parse(RegistryStubs.REGISTERED_AT));

        registry.verify(postRequestedFor(urlEqualTo("/api/v1/registrations"))
                .withRequestBody(matchingJsonPath("$.contractId", equalTo(contract.getId().toString())))
                .withRequestBody(matchingJsonPath("$.contractNumber", equalTo(contract.getContractNumber()))));
    }

    @Test
    void send_contractAlreadyRegistered_marksRegisteredWithSameRecordId() {
        ContractRegistration claimed = claimRegistrationOf(saveIssuedContract().getId());
        RegistryStubs.respondWithRegistration(registry, 200);

        contractRegistrationSender.send(claimed);

        ContractRegistration registration = reload(claimed);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(registration.getRegistryRecordId()).isEqualTo(RegistryStubs.REGISTRY_RECORD_ID);
    }

    @Test
    void send_businessRejection_marksRejected() {
        ContractRegistration claimed = claimRegistrationOf(saveIssuedContract().getId());
        RegistryStubs.respondWithRejection(registry);

        contractRegistrationSender.send(claimed);

        ContractRegistration registration = reload(claimed);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REJECTED);
        assertThat(registration.getLastError()).contains("REGISTRATION_REJECTED");
    }

    @Test
    void send_clientError_marksFailed() {
        ContractRegistration claimed = claimRegistrationOf(saveIssuedContract().getId());
        RegistryStubs.respond(registry, aResponse().withStatus(404));

        contractRegistrationSender.send(claimed);

        ContractRegistration registration = reload(claimed);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.FAILED);
        assertThat(registration.getLastError()).contains("404");
    }

    @Test
    void send_registryUnavailable_keepsPendingAndSchedulesRetry() {
        ContractRegistration claimed = claimRegistrationOf(saveIssuedContract().getId());
        RegistryStubs.respond(registry, aResponse().withStatus(503));
        Instant beforeSend = Instant.now();

        contractRegistrationSender.send(claimed);

        ContractRegistration registration = reload(claimed);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(registration.getLastError()).contains("503");
        assertThat(registration.getNextAttemptAt()).isAfter(beforeSend);
    }

    @Test
    void send_registrationClaimedAgainMeanwhile_discardsOutcome() {
        ContractRegistration staleClaim = claimRegistrationOf(saveIssuedContract().getId());
        claimRegistrationOf(staleClaim.getContractId());
        RegistryStubs.respondWithRegistration(registry, 201);

        contractRegistrationSender.send(staleClaim);

        ContractRegistration registration = reload(staleClaim);
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(registration.getAttempts()).isEqualTo(2);
        assertThat(registration.getRegistryRecordId()).isNull();
    }

    private ContractRegistration claimRegistrationOf(UUID contractId) {
        ContractRegistration registration = contractRegistrationRepository.findByContractId(contractId).orElseThrow();
        registration.startAttempt(Instant.now().plusSeconds(30));
        return contractRegistrationRepository.save(registration);
    }

    private ContractRegistration reload(ContractRegistration registration) {
        return contractRegistrationRepository.findById(registration.getId()).orElseThrow();
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
