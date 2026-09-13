package io.github.dmitrypoverov.insurance.contracts;

import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract contract) {
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
                contract.getIssuedBySubject());
    }
}
