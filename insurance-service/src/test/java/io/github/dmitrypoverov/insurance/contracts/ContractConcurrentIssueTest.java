package io.github.dmitrypoverov.insurance.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import io.github.dmitrypoverov.insurance.support.TestJwtTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

class ContractConcurrentIssueTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";
    private static final String UNDERWRITER_SUBJECT = "33333333-3333-3333-3333-333333333333";
    private static final int REQUEST_COUNT = 8;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Test
    void issue_eightConcurrentRequests_createExactlyOneContract() throws Exception {
        UUID applicationId = saveApprovedApplication();
        CountDownLatch startSignal = new CountDownLatch(1);

        List<IssueOutcome> outcomes = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT)) {
            List<Future<IssueOutcome>> requests = new ArrayList<>();
            for (int i = 0; i < REQUEST_COUNT; i++) {
                requests.add(executor.submit(() -> issueAfterSignal(applicationId, startSignal)));
            }

            startSignal.countDown();

            for (Future<IssueOutcome> request : requests) {
                outcomes.add(request.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(outcomes)
                .extracting(IssueOutcome::status)
                .containsExactlyInAnyOrder(201, 200, 200, 200, 200, 200, 200, 200);

        String firstContractNumber = outcomes.getFirst().contractNumber();
        assertThat(outcomes)
                .extracting(IssueOutcome::contractNumber)
                .containsOnly(firstContractNumber);

        assertThat(contractRepository.findAll()).hasSize(1);
        assertThat(contractRegistrationRepository.findAll()).hasSize(1);
    }

    private IssueOutcome issueAfterSignal(UUID applicationId, CountDownLatch startSignal) throws InterruptedException {
        startSignal.await();

        EntityExchangeResult<String> result = client.post()
                .uri("/api/v1/applications/{id}/contract", applicationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwtTokens.tokenFor(UNDERWRITER_SUBJECT, "underwriter"))
                .exchange()
                .returnResult(String.class);

        return new IssueOutcome(result.getStatus().value(), result.getResponseBody());
    }

    private UUID saveApprovedApplication() {
        Application application = Application.submit(
                CUSTOMER_SUBJECT,
                "Ivan Petrov",
                LocalDate.now().minusYears(36).minusDays(1),
                "AB1234567",
                new BigDecimal("1000000.00"),
                10,
                new BigDecimal("65000.00"));
        application.approve(UNDERWRITER_SUBJECT, Instant.now());
        return applicationRepository.save(application).getId();
    }

    private record IssueOutcome(int status, String body) {

        String contractNumber() {
            return JsonPath.read(body, "$.contractNumber");
        }
    }
}
