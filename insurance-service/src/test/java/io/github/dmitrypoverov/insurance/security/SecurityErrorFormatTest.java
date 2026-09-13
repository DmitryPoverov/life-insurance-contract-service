package io.github.dmitrypoverov.insurance.security;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SecurityErrorFormatTest extends IntegrationTest {

    @Test
    void request_withoutToken_returnsProblemDetailWithCode() {
        client.get()
                .uri("/api/v1/applications")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void request_withMalformedToken_returnsProblemDetailWithCode() {
        client.get()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer garbage")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }
}
