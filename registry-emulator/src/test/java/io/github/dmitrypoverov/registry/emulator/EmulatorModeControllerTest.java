package io.github.dmitrypoverov.registry.emulator;

import io.github.dmitrypoverov.registry.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class EmulatorModeControllerTest extends IntegrationTest {

    private static final int DEFAULT_SLOW_DELAY_SECONDS = 10;

    @Test
    void current_afterStart_returnsSuccess() {
        client.get()
                .uri("/api/v1/emulator/mode")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("SUCCESS");
    }

    @Test
    void switchMode_slowWithDelay_returnsNewMode() {
        switchMode("""
                { "mode": "SLOW", "slowDelaySeconds": 5 }
                """)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("SLOW")
                .jsonPath("$.slowDelaySeconds").isEqualTo(5);

        client.get()
                .uri("/api/v1/emulator/mode")
                .exchange()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("SLOW");
    }

    @Test
    void switchMode_withoutDelay_usesDefaultDelay() {
        switchMode("""
                { "mode": "SLOW" }
                """)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.slowDelaySeconds").isEqualTo(DEFAULT_SLOW_DELAY_SECONDS);
    }

    @Test
    void switchMode_whileUnavailable_stillResponds() {
        switchMode("""
                { "mode": "UNAVAILABLE" }
                """)
                .expectStatus().isOk();

        switchMode("""
                { "mode": "SUCCESS" }
                """)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("SUCCESS");
    }

    @Test
    void switchMode_withoutMode_returnsValidationFailed() {
        switchMode("""
                { "slowDelaySeconds": 5 }
                """)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors.mode").exists();
    }

    private RestTestClient.ResponseSpec switchMode(String body) {
        return client.post()
                .uri("/api/v1/emulator/mode")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }
}
