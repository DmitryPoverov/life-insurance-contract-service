package io.github.dmitrypoverov.insurance.registrations;

import io.github.dmitrypoverov.insurance.contracts.Contract;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RegistryClient {

    private static final String REJECTION_CODE = "REGISTRATION_REJECTED";

    private final RegistryApi registryApi;

    public RegistryRecord register(Contract contract) {
        RegistryRegistrationResponse response = send(toRequest(contract));
        String registryRecordId = response != null ? response.registryRecordId() : null;
        Instant registeredAt = response != null ? response.registeredAt() : null;

        if (registryRecordId == null || registeredAt == null) {
            throw new RetryableRegistryException("Registry returned a success response without the record");
        }
        return new RegistryRecord(registryRecordId, registeredAt);
    }

    private @Nullable RegistryRegistrationResponse send(RegistryRegistrationRequest request) {
        try {
            return registryApi.register(request);
        } catch (RestClientResponseException errorResponse) {
            throw classify(errorResponse);
        } catch (RestClientException transportOrBodyError) {
            throw new RetryableRegistryException(transportOrBodyError);
        }
    }

    private RuntimeException classify(RestClientResponseException errorResponse) {
        if (isBusinessRejection(errorResponse)) {
            return new RegistryRejectedException(errorResponse);
        }
        boolean clientError = errorResponse.getStatusCode().is4xxClientError();
        boolean tooManyRequests = errorResponse.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS);
        if (clientError && !tooManyRequests) {
            return new PermanentRegistryException(errorResponse);
        }
        return new RetryableRegistryException(errorResponse);
    }

    private boolean isBusinessRejection(RestClientResponseException errorResponse) {
        if (!errorResponse.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_CONTENT)) {
            return false;
        }
        try {
            RegistryProblem problem = errorResponse.getResponseBodyAs(RegistryProblem.class);
            return problem != null && REJECTION_CODE.equals(problem.code());
        } catch (RestClientException unreadableBody) {
            return false;
        }
    }

    private RegistryRegistrationRequest toRequest(Contract contract) {
        return new RegistryRegistrationRequest(
                contract.getId(),
                contract.getContractNumber(),
                contract.getInsuredFullName(),
                contract.getInsuredBirthDate(),
                contract.getInsuredDocumentNumber(),
                contract.getCoverageAmount(),
                contract.getPremium(),
                contract.getStartDate(),
                contract.getEndDate());
    }
}
