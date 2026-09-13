package io.github.dmitrypoverov.insurance.contracts;

import java.util.UUID;

public class ContractIssuanceBusyException extends RuntimeException {

    public ContractIssuanceBusyException(UUID applicationId, Throwable cause) {
        super("Contract issuance for application %s is in progress, retry later".formatted(applicationId), cause);
    }
}
