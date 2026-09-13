package io.github.dmitrypoverov.insurance.contracts;

import java.util.UUID;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(UUID id) {
        super("Contract %s not found".formatted(id));
    }
}
