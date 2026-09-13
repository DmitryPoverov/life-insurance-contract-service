package io.github.dmitrypoverov.insurance.contracts;

public record ContractIssueResult(Contract contract, boolean created) {

    static ContractIssueResult newlyIssued(Contract contract) {
        return new ContractIssueResult(contract, true);
    }

    static ContractIssueResult alreadyIssued(Contract contract) {
        return new ContractIssueResult(contract, false);
    }
}
