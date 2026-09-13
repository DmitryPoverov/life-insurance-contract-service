package io.github.dmitrypoverov.insurance.registrations;

public class RegistryRejectedException extends RuntimeException {

    public RegistryRejectedException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
