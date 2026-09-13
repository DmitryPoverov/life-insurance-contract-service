package io.github.dmitrypoverov.insurance.applications;

import java.util.UUID;

public class ApplicationStatusTransitionException extends RuntimeException {

    public ApplicationStatusTransitionException(UUID applicationId,
                                                ApplicationStatus from,
                                                ApplicationStatus to) {
        super("Application %s: transition %s -> %s is not allowed".formatted(applicationId, from, to));
    }
}
