package io.github.dmitrypoverov.registry.registrations;

public class EmulatedUnavailableException extends RuntimeException {

    public EmulatedUnavailableException() {
        super("Emulated registry unavailability");
    }
}
