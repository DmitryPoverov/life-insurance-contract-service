package io.github.dmitrypoverov.insurance.applications;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("Application %s not found".formatted(id));
    }
}
