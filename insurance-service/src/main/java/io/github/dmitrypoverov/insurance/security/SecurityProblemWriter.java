package io.github.dmitrypoverov.insurance.security;

import io.github.dmitrypoverov.insurance.web.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SecurityProblemWriter {

    private static final String CODE_PROPERTY = "code";

    static void write(HttpServletResponse response, HttpStatus status, ErrorCode code, String detail,
                      ObjectMapper objectMapper) throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty(CODE_PROPERTY, code.name());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
