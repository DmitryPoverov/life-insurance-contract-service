package io.github.dmitrypoverov.insurance.registrations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistryRegistrationRequest(
        UUID contractId,
        String contractNumber,
        String insuredFullName,
        LocalDate insuredBirthDate,
        String insuredDocumentNumber,
        BigDecimal coverageAmount,
        BigDecimal premium,
        LocalDate startDate,
        LocalDate endDate) {
}
