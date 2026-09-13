package io.github.dmitrypoverov.registry.registrations;

public class EmulatedServerErrorException extends RuntimeException {

    public EmulatedServerErrorException() {
        super("Emulated registry internal error");
    }
}
