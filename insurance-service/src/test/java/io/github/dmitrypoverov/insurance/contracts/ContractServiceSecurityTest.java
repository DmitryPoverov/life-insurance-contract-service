package io.github.dmitrypoverov.insurance.contracts;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ContractServiceSecurityTest extends IntegrationTest {

    private static final String CUSTOMER_SUBJECT = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private ContractService contractService;

    @BeforeEach
    void authenticateAsCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(CUSTOMER_SUBJECT, "n/a", "ROLE_CUSTOMER"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAnyById_customerAuthentication_throwsAccessDenied() {
        UUID contractId = UUID.randomUUID();

        assertThatThrownBy(() -> contractService.getAnyById(contractId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findAny_customerAuthentication_throwsAccessDenied() {
        ContractFilter emptyFilter = new ContractFilter(null, null, null, null);
        Pageable firstPage = Pageable.ofSize(20);

        assertThatThrownBy(() -> contractService.findAny(emptyFilter, firstPage))
                .isInstanceOf(AccessDeniedException.class);
    }
}
