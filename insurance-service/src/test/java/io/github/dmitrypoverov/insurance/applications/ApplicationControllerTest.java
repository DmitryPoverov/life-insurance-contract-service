package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    @Test
    void list_customer_returnsOnlyOwnApplications() {
        saveApplicationOf(CUSTOMER_SUBJECT);
        saveApplicationOf(OTHER_CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].applicantSubject").isEqualTo(CUSTOMER_SUBJECT);
    }

    @Test
    void list_underwriter_returnsAllApplications() {
        saveApplicationOf(CUSTOMER_SUBJECT);
        saveApplicationOf(OTHER_CUSTOMER_SUBJECT);

        client.get()
                .uri("/api/v1/applications")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void list_withStatusAndCoverageFilters_returnsMatchingOnly() {
        applicationRepository.save(newApplication(CUSTOMER_SUBJECT, new BigDecimal("1000000.00")));
        Application approvedSmall = newApplication(CUSTOMER_SUBJECT, new BigDecimal("50000.00"));
        approvedSmall.approve(UNDERWRITER_SUBJECT, Instant.now());
        applicationRepository.save(approvedSmall);
        Application approvedLarge = newApplication(CUSTOMER_SUBJECT, new BigDecimal("1000000.00"));
        approvedLarge.approve(UNDERWRITER_SUBJECT, Instant.now());
        Application expected = applicationRepository.save(approvedLarge);

        client.get()
                .uri("/api/v1/applications?status=APPROVED&coverageFrom=100000")
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(expected.getId().toString());
    }

    @Test
    void list_withCreatedDateRange_filtersByCreationDay() {
        saveApplicationOf(CUSTOMER_SUBJECT);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        client.get()
                .uri("/api/v1/applications?createdFrom={from}", today)
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(1);

        client.get()
                .uri("/api/v1/applications?createdTo={to}", today.minusDays(1))
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void list_unsupportedSortProperty_returnsBadRequest() {
        client.get()
                .uri("/api/v1/applications?sort=insuredFullName")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_SORT");
    }

    @Test
    void list_withUnconvertibleParameter_returnsValidationFailedWithoutInternalDetails() {
        client.get()
                .uri("/api/v1/applications?status=FOO")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors.status").isEqualTo("invalid value");
    }

    @Test
    void list_pageSizeAboveMaximum_clampsToMaximum() {
        client.get()
                .uri("/api/v1/applications?size=1000")
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page.size").isEqualTo(100);
    }

    @Test
    void approve_submittedApplication_returnsApproved() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.post()
                .uri("/api/v1/applications/{id}/approve", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.decidedBySubject").isEqualTo(UNDERWRITER_SUBJECT)
                .jsonPath("$.decidedAt").exists();
    }

    @Test
    void approve_withCustomerToken_returnsForbidden() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.post()
                .uri("/api/v1/applications/{id}/approve", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(CUSTOMER_SUBJECT, "customer"))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCESS_DENIED");
    }

    @Test
    void approve_nonExistentId_returnsNotFound() {
        client.post()
                .uri("/api/v1/applications/{id}/approve", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void approve_rejectedApplication_returnsConflict() {
        Application application = newApplication(CUSTOMER_SUBJECT, new BigDecimal("1000000.00"));
        application.reject(UNDERWRITER_SUBJECT, "Incomplete documents", Instant.now());
        Application saved = applicationRepository.save(application);

        client.post()
                .uri("/api/v1/applications/{id}/approve", saved.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");
    }

    @Test
    void reject_submittedApplication_returnsRejectedWithReason() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.post()
                .uri("/api/v1/applications/{id}/reject", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "reason": "Incomplete documents" }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REJECTED")
                .jsonPath("$.rejectionReason").isEqualTo("Incomplete documents");
    }

    @Test
    void reject_withBlankReason_returnsValidationFailed() {
        Application application = saveApplicationOf(CUSTOMER_SUBJECT);

        client.post()
                .uri("/api/v1/applications/{id}/reject", application.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "reason": " " }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors.reason").exists();
    }

    @Test
    void reject_approvedApplication_returnsConflict() {
        Application application = newApplication(CUSTOMER_SUBJECT, new BigDecimal("1000000.00"));
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        Application saved = applicationRepository.save(application);

        client.post()
                .uri("/api/v1/applications/{id}/reject", saved.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(UNDERWRITER_SUBJECT, "underwriter"))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "reason": "Changed my mind" }
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");
    }

    private Application saveApplicationOf(String applicantSubject) {
        return applicationRepository.save(newApplication(applicantSubject, new BigDecimal("1000000.00")));
    }

    private Application newApplication(String applicantSubject, BigDecimal coverageAmount) {
        return Application.submit(
                applicantSubject,
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                coverageAmount,
                10,
                new BigDecimal("65000.00"));
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
