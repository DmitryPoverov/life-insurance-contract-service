package io.github.dmitrypoverov.registry.registrations;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrationRequest(
        @NotNull UUID contractId,
        @NotBlank @Size(max = 32) String contractNumber,
        @NotBlank @Size(max = 255) String insuredFullName,
        @NotNull @Past LocalDate insuredBirthDate,
        @NotBlank @Size(max = 64) String insuredDocumentNumber,
        @NotNull @Positive BigDecimal coverageAmount,
        @NotNull @PositiveOrZero BigDecimal premium,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
