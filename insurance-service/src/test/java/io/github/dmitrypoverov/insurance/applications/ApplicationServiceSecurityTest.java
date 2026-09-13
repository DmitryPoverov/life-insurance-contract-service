package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ApplicationServiceSecurityTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private ApplicationService applicationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAnyById_customerAuthentication_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(CUSTOMER_SUBJECT, "n/a", "ROLE_CUSTOMER"));

        UUID uuid = UUID.randomUUID();
        assertThatThrownBy(() -> applicationService.getAnyById(uuid))
                .isInstanceOf(AccessDeniedException.class);
    }
}
