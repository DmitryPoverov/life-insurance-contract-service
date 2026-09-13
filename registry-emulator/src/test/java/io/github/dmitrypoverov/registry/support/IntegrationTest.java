package io.github.dmitrypoverov.registry.support;

import io.github.dmitrypoverov.registry.emulator.EmulatorMode;
import io.github.dmitrypoverov.registry.emulator.EmulatorModeSwitch;
import io.github.dmitrypoverov.registry.registrations.RegistrationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EmulatorModeSwitch emulatorModeSwitch;

    @LocalServerPort
    private int port;

    protected RestTestClient client;

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAllInBatch();
        emulatorModeSwitch.switchTo(EmulatorMode.SUCCESS, null);
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    protected RestTestClient.ResponseSpec register(UUID contractId) {
        return client.post()
                .uri("/api/v1/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registrationJson(contractId))
                .exchange();
    }

    protected static String registrationJson(UUID contractId) {
        return """
                {
                  "contractId": "%s",
                  "contractNumber": "LI-2026-000042",
                  "insuredFullName": "Ivan Petrov",
                  "insuredBirthDate": "1990-05-17",
                  "insuredDocumentNumber": "AB1234567",
                  "coverageAmount": 1000000.00,
                  "premium": 65000.00,
                  "startDate": "2026-09-13",
                  "endDate": "2036-09-12"
                }
                """.formatted(contractId);
    }
}
