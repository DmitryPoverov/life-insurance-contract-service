package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
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
        boolean pending = registration.getStatus() == RegistrationStatus.PENDING;
        return new ContractRegistrationResponse(
                registration.getStatus(),
                registration.getAttempts(),
                pending ? registration.getNextAttemptAt() : null,
                registration.getRegistryRecordId(),
                registration.getRegisteredAt());
    }
}
