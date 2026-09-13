package io.github.dmitrypoverov.insurance.web;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CurrentUserControllerTest extends IntegrationTest {

    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Test
    void me_withoutToken_returnsUnauthorized() {
        client.get().uri("/api/v1/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void me_withUnderwriterToken_returnsSubjectAndRole() {
        String token = TestJwtTokens.tokenFor(UNDERWRITER_SUBJECT, "underwriter");

        client
                .get()
                .uri("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.sub")
                .isEqualTo(UNDERWRITER_SUBJECT)
                .jsonPath("$.roles[0]")
                .isEqualTo("ROLE_UNDERWRITER");
    }
}
