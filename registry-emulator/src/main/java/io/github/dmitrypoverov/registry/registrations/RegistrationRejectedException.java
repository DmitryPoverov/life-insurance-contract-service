package io.github.dmitrypoverov.registry.registrations;

import java.util.UUID;

public class RegistrationRejectedException extends RuntimeException {

    public RegistrationRejectedException(UUID contractId) {
        super("Registry rejected contract %s".formatted(contractId));
    }
}
