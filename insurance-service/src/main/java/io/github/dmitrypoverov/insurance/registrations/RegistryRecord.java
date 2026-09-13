package io.github.dmitrypoverov.insurance.registrations;

import java.time.Instant;

public record RegistryRecord(String registryRecordId, Instant registeredAt) {
}
