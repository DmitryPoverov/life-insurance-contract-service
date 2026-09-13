package io.github.dmitrypoverov.insurance.applications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String applicantSubject,
        String insuredFullName,
        LocalDate insuredBirthDate,
        String insuredDocumentNumber,
        BigDecimal coverageAmount,
        int termYears,
        BigDecimal calculatedPremium,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant decidedAt,
        String decidedBySubject,
        String rejectionReason) {
}
