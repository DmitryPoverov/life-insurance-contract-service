package io.github.dmitrypoverov.registry.web;

import io.github.dmitrypoverov.registry.registrations.EmulatedServerErrorException;
import io.github.dmitrypoverov.registry.registrations.EmulatedUnavailableException;
import io.github.dmitrypoverov.registry.registrations.RegistrationNotFoundException;
import io.github.dmitrypoverov.registry.registrations.RegistrationRejectedException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE_PROPERTY = "code";
    private static final String ERRORS_PROPERTY = "errors";
    private static final String INVALID_VALUE = "invalid value";

    @ExceptionHandler(RegistrationNotFoundException.class)
    @Nullable ResponseEntity<Object> handleNotFound(RegistrationNotFoundException exception,
                                                    WebRequest request) {
        return problem(exception, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(RegistrationRejectedException.class)
    @Nullable ResponseEntity<Object> handleRejected(RegistrationRejectedException exception,
                                                    WebRequest request) {
        return problem(exception, HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.REGISTRATION_REJECTED, request);
    }

    @ExceptionHandler(EmulatedServerErrorException.class)
    @Nullable ResponseEntity<Object> handleServerError(EmulatedServerErrorException exception,
                                                       WebRequest request) {
        return problem(exception, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.TECHNICAL_ERROR, request);
    }

    @ExceptionHandler(EmulatedUnavailableException.class)
    @Nullable ResponseEntity<Object> handleUnavailable(EmulatedUnavailableException exception,
                                                       WebRequest request) {
        return problem(exception, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        body.setProperty(CODE_PROPERTY, ErrorCode.VALIDATION_FAILED.name());
        body.setProperty(ERRORS_PROPERTY, fieldErrors(exception));
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    private @Nullable ResponseEntity<Object> problem(Exception exception,
                                                     HttpStatus status,
                                                     ErrorCode code,
                                                     WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        body.setProperty(CODE_PROPERTY, code.name());
        return handleExceptionInternal(exception, body, new HttpHeaders(), status, request);
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), messageFor(error));
        }
        return errors;
    }

    private String messageFor(FieldError error) {
        if (error.isBindingFailure()) {
            return INVALID_VALUE;
        }
        String message = error.getDefaultMessage();
        return message != null ? message : INVALID_VALUE;
    }
}
