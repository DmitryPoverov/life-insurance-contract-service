package io.github.dmitrypoverov.insurance.support;

import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
public abstract class IntegrationTest {

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @LocalServerPort
    private int port;

    protected RestTestClient client;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private void cleanDatabase() {
        contractRegistrationRepository.deleteAllInBatch();
        contractRepository.deleteAllInBatch();
        applicationRepository.deleteAllInBatch();
    }
}
