package io.github.dmitrypoverov.registry.registrations;

public record RegistrationResult(Registration registration, boolean created) {

    static RegistrationResult newlyRegistered(Registration registration) {
        return new RegistrationResult(registration, true);
    }

    static RegistrationResult alreadyRegistered(Registration registration) {
        return new RegistrationResult(registration, false);
    }
}
