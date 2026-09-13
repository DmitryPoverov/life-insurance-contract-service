package io.github.dmitrypoverov.insurance.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
public abstract class IntegrationTest {

    protected static final WireMockServer registry = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        registry.start();
    }

    @Autowired
    private ContractRegistrationRepository contractRegistrationRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @LocalServerPort
    private int port;

    protected RestTestClient client;

    @DynamicPropertySource
    static void registryProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.http.serviceclient.registry.base-url", registry::baseUrl);
        properties.add("spring.http.serviceclient.registry.read-timeout", () -> "500ms");
        properties.add("insurance.registration.scheduler.poll-interval", () -> "1h");
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
        registry.resetAll();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private void cleanDatabase() {
        contractRegistrationRepository.deleteAllInBatch();
        contractRepository.deleteAllInBatch();
        applicationRepository.deleteAllInBatch();
    }
}
