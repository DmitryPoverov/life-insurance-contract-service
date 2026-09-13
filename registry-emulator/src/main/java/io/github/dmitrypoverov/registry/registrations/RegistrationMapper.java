package io.github.dmitrypoverov.registry.registrations;

import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getRegistryRecordId(),
                registration.getContractId(),
                registration.getContractNumber(),
                registration.getInsuredFullName(),
                registration.getInsuredBirthDate(),
                registration.getInsuredDocumentNumber(),
                registration.getCoverageAmount(),
                registration.getPremium(),
                registration.getStartDate(),
                registration.getEndDate(),
                registration.getRegisteredAt());
    }
}
