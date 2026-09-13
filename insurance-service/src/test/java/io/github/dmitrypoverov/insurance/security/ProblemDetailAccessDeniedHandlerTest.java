package io.github.dmitrypoverov.insurance.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dmitrypoverov.insurance.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class ProblemDetailAccessDeniedHandlerTest extends IntegrationTest {

    @Autowired
    private ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @Test
    void handle_deniedRequest_writesProblemDetailWithCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
    }
}
