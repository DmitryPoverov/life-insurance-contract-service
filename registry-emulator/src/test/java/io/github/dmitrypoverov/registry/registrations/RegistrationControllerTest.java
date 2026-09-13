package io.github.dmitrypoverov.registry.registrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.registry.emulator.EmulatorMode;
import io.github.dmitrypoverov.registry.emulator.EmulatorModeSwitch;
import io.github.dmitrypoverov.registry.support.IntegrationTest;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RegistrationControllerTest extends IntegrationTest {

    private static final String REGISTRY_RECORD_ID_FORMAT = "GSR-\\d{4}-\\d{6}";

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EmulatorModeSwitch emulatorModeSwitch;

    @Test
    void register_newContract_returnsCreatedWithRecord() {
        UUID contractId = UUID.randomUUID();

        register(contractId)
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.LOCATION, "/api/v1/registrations/" + contractId)
                .expectBody()
                .jsonPath("$.contractId").isEqualTo(contractId.toString())
                .jsonPath("$.contractNumber").isEqualTo("LI-2026-000042")
                .jsonPath("$.registeredAt").exists()
                .jsonPath("$.registryRecordId")
                .value(id -> assertThat(id.toString()).matches(REGISTRY_RECORD_ID_FORMAT));
    }

    @Test
    void register_sameContractTwice_returnsOkWithSameRecord() {
        UUID contractId = UUID.randomUUID();
        register(contractId).expectStatus().isCreated();
        Registration first = registrationRepository.findByContractId(contractId).orElseThrow();

        register(contractId)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.registryRecordId").isEqualTo(first.getRegistryRecordId())
                .jsonPath("$.registeredAt").isEqualTo(first.getRegisteredAt().toString());

        assertThat(registrationRepository.findAll()).hasSize(1);
    }

    @Test
    void register_invalidRequest_returnsValidationFailed() {
        client.post()
                .uri("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "contractNumber": "LI-2026-000042" }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors.contractId").exists();
    }

    @Test
    void register_businessErrorMode_returnsUnprocessableWithRejectionCode() {
        emulatorModeSwitch.switchTo(EmulatorMode.BUSINESS_ERROR, null);

        register(UUID.randomUUID())
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.code").isEqualTo("REGISTRATION_REJECTED");

        assertThat(registrationRepository.findAll()).isEmpty();
    }

    @Test
    void register_technicalErrorMode_returnsInternalServerError() {
        emulatorModeSwitch.switchTo(EmulatorMode.TECHNICAL_ERROR, null);

        register(UUID.randomUUID())
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.code").isEqualTo("TECHNICAL_ERROR");

        assertThat(registrationRepository.findAll()).isEmpty();
    }

    @Test
    void register_unavailableMode_returnsServiceUnavailable() {
        emulatorModeSwitch.switchTo(EmulatorMode.UNAVAILABLE, null);

        register(UUID.randomUUID())
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE");

        assertThat(registrationRepository.findAll()).isEmpty();
    }

    @Test
    void register_alreadyRegisteredContractInBusinessErrorMode_returnsOkWithExistingRecord() {
        UUID contractId = UUID.randomUUID();
        register(contractId).expectStatus().isCreated();

        emulatorModeSwitch.switchTo(EmulatorMode.BUSINESS_ERROR, null);

        register(contractId)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.contractId").isEqualTo(contractId.toString());
    }

    @Test
    void register_slowMode_commitsRecordBeforeResponding() {
        UUID contractId = UUID.randomUUID();
        emulatorModeSwitch.switchTo(EmulatorMode.SLOW, Duration.ofSeconds(3));

        CompletableFuture<Void> request = CompletableFuture.runAsync(
                () -> register(contractId).expectStatus().isCreated());

        assertThatThrownBy(() -> request.get(1, TimeUnit.SECONDS))
                .as("response must be held back by the slow mode")
                .isInstanceOf(TimeoutException.class);
        assertThat(registrationRepository.findByContractId(contractId))
                .as("record must be committed while the response is still pending")
                .isPresent();

        assertThat(request).succeedsWithin(Duration.ofSeconds(10));
    }

    @Test
    void getByContractId_registeredContract_returnsRecord() {
        UUID contractId = UUID.randomUUID();
        register(contractId).expectStatus().isCreated();

        client.get()
                .uri("/api/v1/registrations/{contractId}", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.contractId").isEqualTo(contractId.toString());
    }

    @Test
    void getByContractId_unknownContract_returnsNotFound() {
        client.get()
                .uri("/api/v1/registrations/{contractId}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void list_filteredByContractIdAfterRepeatedRegistration_returnsSingleRecord() {
        UUID contractId = UUID.randomUUID();
        register(contractId).expectStatus().isCreated();
        register(contractId).expectStatus().isOk();
        register(contractId).expectStatus().isOk();
        register(UUID.randomUUID()).expectStatus().isCreated();

        client.get()
                .uri("/api/v1/registrations?contractId={contractId}", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].contractId").isEqualTo(contractId.toString());
    }

    @Test
    void readEndpoints_unavailableMode_stillRespond() {
        UUID contractId = UUID.randomUUID();
        register(contractId).expectStatus().isCreated();
        emulatorModeSwitch.switchTo(EmulatorMode.UNAVAILABLE, null);

        client.get()
                .uri("/api/v1/registrations/{contractId}", contractId)
                .exchange()
                .expectStatus().isOk();
        client.get()
                .uri("/api/v1/registrations")
                .exchange()
                .expectStatus().isOk();
    }
}
