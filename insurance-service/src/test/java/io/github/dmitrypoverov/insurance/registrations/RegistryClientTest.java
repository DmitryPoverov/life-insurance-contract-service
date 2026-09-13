package io.github.dmitrypoverov.insurance.registrations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.contracts.Contract;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.RegistryStubs;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class RegistryClientTest extends IntegrationTest {

    private final Contract contract = unsavedContract();

    @Autowired
    private RegistryClient registryClient;

    @ParameterizedTest
    @ValueSource(ints = {200, 201})
    void register_successResponse_returnsRegistryRecord(int status) {
        RegistryStubs.respondWithRegistration(registry, status);

        RegistryRecord registryRecord = registryClient.register(contract);

        assertThat(registryRecord.registryRecordId()).isEqualTo(RegistryStubs.REGISTRY_RECORD_ID);
        assertThat(registryRecord.registeredAt()).isEqualTo(Instant.parse(RegistryStubs.REGISTERED_AT));
    }

    @Test
    void register_businessRejection_throwsRegistryRejected() {
        RegistryStubs.respondWithRejection(registry);

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(RegistryRejectedException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void register_clientErrorWithoutRejectionCode_throwsPermanent(int status) {
        RegistryStubs.respond(registry, aResponse().withStatus(status));

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(PermanentRegistryException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void register_serverErrorOrTooManyRequests_throwsRetryable(int status) {
        RegistryStubs.respond(registry, aResponse().withStatus(status));

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(RetryableRegistryException.class);
    }

    @Test
    void register_responseSlowerThanReadTimeout_throwsRetryable() {
        RegistryStubs.respond(registry, aResponse().withStatus(201).withFixedDelay(1500));

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(RetryableRegistryException.class);
    }

    @Test
    void register_connectionReset_throwsRetryable() {
        RegistryStubs.respond(registry, aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER));

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(RetryableRegistryException.class);
    }

    @Test
    void register_unreadableSuccessBody_throwsRetryable() {
        RegistryStubs.respond(registry, aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("not json"));

        assertThatThrownBy(() -> registryClient.register(contract))
                .isInstanceOf(RetryableRegistryException.class);
    }

    private static Contract unsavedContract() {
        Application application = Application.submit(
                "11111111-1111-1111-1111-111111111111",
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("65000.00"));
        return Contract.issue(application, "33333333-3333-3333-3333-333333333333", Instant.now(), LocalDate.now());
    }
}
