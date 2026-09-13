package io.github.dmitrypoverov.registry.registrations;

import java.util.UUID;

public class RegistrationNotFoundException extends RuntimeException {

    public RegistrationNotFoundException(UUID contractId) {
        super("Registration for contract %s not found".formatted(contractId));
    }
}
