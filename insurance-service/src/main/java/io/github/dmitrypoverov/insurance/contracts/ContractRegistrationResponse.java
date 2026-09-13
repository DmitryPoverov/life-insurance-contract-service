package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record ContractRegistrationResponse(
        RegistrationStatus status,
        int attempts,
        @Nullable Instant nextAttemptAt,
        @Nullable String registryRecordId,
        @Nullable Instant registeredAt) {
}
