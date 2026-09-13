package io.github.dmitrypoverov.insurance.registrations;

public class RetryableRegistryException extends RuntimeException {

    public RetryableRegistryException(String message) {
        super(message);
    }

    public RetryableRegistryException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
