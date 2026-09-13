package io.github.dmitrypoverov.insurance.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID applicationId,
        String contractNumber,
        String policyholderSubject,
        String insuredFullName,
        LocalDate insuredBirthDate,
        String insuredDocumentNumber,
        BigDecimal coverageAmount,
        BigDecimal premium,
        LocalDate startDate,
        LocalDate endDate,
        Instant issuedAt,
        String issuedBySubject,
        ContractRegistrationResponse registration) {
}
