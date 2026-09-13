package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ApplicationControllerTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_CUSTOMER_SUBJECT = "22222222-2222-2222-2222-222222222222";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ApplicationRepository applicationRepository;

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

    @Test
    void getById_ownApplication_returnsOk() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/applications/{id}", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(application.getId().toString())
                .jsonPath("$.applicantSubject").isEqualTo(CUSTOMER_SUBJECT);
    }

    @Test
    void getById_otherCustomersApplication_returnsNotFound() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/applications/{id}", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(OTHER_CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void getById_underwriter_returnsOk() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/applications/{id}", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(application.getId().toString());
    }

    @Test
    void getById_nonExistentId_returnsNotFound() {
        client.get()
                .uri("/api/v1/applications/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void getById_malformedId_returnsBadRequest() {
        client.get()
                .uri("/api/v1/applications/not-a-uuid")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST");
    }

    private Application saveApplicationOf(String applicantSubject) {
        return applicationRepository.save(Application.submit(
                applicantSubject,
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("65000.00")));
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
