package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(ContractDetails details) {
        Contract contract = details.contract();
        return new ContractResponse(
                contract.getId(),
                contract.getApplication().getId(),
                contract.getContractNumber(),
                contract.getPolicyholderSubject(),
                contract.getInsuredFullName(),
                contract.getInsuredBirthDate(),
                contract.getInsuredDocumentNumber(),
                contract.getCoverageAmount(),
                contract.getPremium(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getIssuedAt(),
                contract.getIssuedBySubject(),
                toRegistrationResponse(details.registration()));
    }

    private ContractRegistrationResponse toRegistrationResponse(ContractRegistration registration) {
        return new ContractRegistrationResponse(
                registration.getStatus(),
                registration.getAttempts(),
                registration.getNextAttemptAt(),
                registration.getRegistryRecordId(),
                registration.getRegisteredAt());
    }
}
