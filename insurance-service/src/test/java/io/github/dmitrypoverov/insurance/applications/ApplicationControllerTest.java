package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ApplicationControllerTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Test
    void create_withCustomerToken_returnsCreatedWithCalculatedPremium() {
        client.post()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestForAge(36))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUBMITTED")
                .jsonPath("$.applicantSubject").isEqualTo(CUSTOMER_SUBJECT)
                .jsonPath("$.calculatedPremium")
                .value(premium -> assertThat(new BigDecimal(premium.toString()))
                        .isEqualByComparingTo("65000.00"));
    }

    @Test
    void create_withUnderwriterToken_returnsForbidden() {
        client.post()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestForAge(36))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    void create_withoutToken_returnsUnauthorized() {
        client.post()
                .uri("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestForAge(36))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_withInvalidFields_returnsValidationFailedWithFieldErrors() {
        String invalidRequest = """
                {
                  "insuredFullName": "",
                  "insuredBirthDate": "2030-01-01",
                  "insuredDocumentNumber": "",
                  "coverageAmount": 100,
                  "termYears": 99
                }
                """;

        client.post()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors.insuredFullName").exists()
                .jsonPath("$.errors.coverageAmount").exists()
                .jsonPath("$.errors.termYears").exists();
    }

    @Test
    void create_withAgeOutsideTariff_returnsAgeNotEligible() {
        client.post()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestForAge(86))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AGE_NOT_ELIGIBLE");
    }

    private String bearer(String subject, String... roles) {
        return "Bearer " + TestJwtTokens.tokenFor(subject, roles);
    }

    private String requestForAge(int age) {
        LocalDate birthDate = LocalDate.now().minusYears(age).minusDays(1);
        return """
                {
                  "insuredFullName": "Ivan Petrov",
                  "insuredBirthDate": "%s",
                  "insuredDocumentNumber": "AB1234567",
                  "coverageAmount": 1000000.00,
                  "termYears": 10
                }
                """.formatted(birthDate);
    }
}
