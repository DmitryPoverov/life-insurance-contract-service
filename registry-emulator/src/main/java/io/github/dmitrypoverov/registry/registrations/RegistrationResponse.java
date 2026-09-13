package io.github.dmitrypoverov.registry.registrations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrationResponse(
        String registryRecordId,
        UUID contractId,
        String contractNumber,
        String insuredFullName,
        LocalDate insuredBirthDate,
        String insuredDocumentNumber,
        BigDecimal coverageAmount,
        BigDecimal premium,
        LocalDate startDate,
        LocalDate endDate,
        Instant registeredAt) {
}
