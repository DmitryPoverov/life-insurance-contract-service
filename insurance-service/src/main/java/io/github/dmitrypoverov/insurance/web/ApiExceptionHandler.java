package io.github.dmitrypoverov.insurance.web;

import io.github.dmitrypoverov.insurance.applications.ApplicantAgeNotEligibleException;
import io.github.dmitrypoverov.insurance.applications.ApplicationStatusTransitionException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE_PROPERTY = "code";
    private static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(AccessDeniedException.class)
    @Nullable ResponseEntity<Object> handleAccessDenied(
            AccessDeniedException exception,
            WebRequest request) {
        return problem(exception, HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Access is denied",
                request);
    }

    @ExceptionHandler(ApplicationStatusTransitionException.class)
    @Nullable ResponseEntity<Object> handleStatusTransition(
            ApplicationStatusTransitionException exception, WebRequest request) {
        return problem(exception, HttpStatus.CONFLICT, ErrorCode.CONFLICT, exception.getMessage(),
                request);
    }


    @ExceptionHandler(Exception.class)
    @Nullable ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        logger.error("Unexpected error while handling request", exception);
        return problem(
                exception,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Internal server error",
                request);
    }

    @ExceptionHandler(ApplicantAgeNotEligibleException.class)
    @Nullable ResponseEntity<Object> handleAgeNotEligible(
            ApplicantAgeNotEligibleException exception, WebRequest request) {
        return problem(
                exception,
                HttpStatus.BAD_REQUEST,
                ErrorCode.AGE_NOT_ELIGIBLE,
                exception.getMessage(),
                request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        body.setProperty(CODE_PROPERTY, ErrorCode.VALIDATION_FAILED.name());
        body.setProperty(ERRORS_PROPERTY, fieldErrors(exception));
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Endpoint not found");
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ResponseEntity<Object> response =
                super.handleExceptionInternal(exception, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            addCodeIfMissing(problem, statusCode);
        }
        return response;
    }

    private @Nullable ResponseEntity<Object> problem(
            Exception exception, HttpStatus status, ErrorCode code, String detail, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty(CODE_PROPERTY, code.name());
        return handleExceptionInternal(exception, body, new HttpHeaders(), status, request);
    }

    private void addCodeIfMissing(ProblemDetail problem, HttpStatusCode statusCode) {
        Map<String, Object> properties = problem.getProperties();
        if (properties != null && properties.containsKey(CODE_PROPERTY)) {
            return;
        }
        problem.setProperty(CODE_PROPERTY, defaultCode(statusCode).name());
    }

    private ErrorCode defaultCode(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> ErrorCode.MALFORMED_REQUEST;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value"));
        }
        return errors;
    }
}
