package io.github.dmitrypoverov.insurance.registrations;

import java.util.UUID;

public class RegistrationNotFailedException extends RuntimeException {

    public RegistrationNotFailedException(UUID contractId, RegistrationStatus actual) {
        super("Registration for contract %s is %s, not FAILED".formatted(contractId, actual));
    }
}
