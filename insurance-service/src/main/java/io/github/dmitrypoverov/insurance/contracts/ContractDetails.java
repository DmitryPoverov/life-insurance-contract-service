package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;

public record ContractDetails(Contract contract, ContractRegistration registration) {
}
