package io.github.dmitrypoverov.insurance.registrations;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record RegistryRegistrationResponse(@Nullable String registryRecordId, @Nullable Instant registeredAt) {
}
