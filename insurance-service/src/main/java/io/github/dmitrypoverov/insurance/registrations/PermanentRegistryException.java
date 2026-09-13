package io.github.dmitrypoverov.insurance.registrations;

public class PermanentRegistryException extends RuntimeException {

    public PermanentRegistryException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
