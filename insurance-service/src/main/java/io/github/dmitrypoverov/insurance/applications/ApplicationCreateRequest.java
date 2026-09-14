package io.github.dmitrypoverov.insurance.applications;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationCreateRequest(
        @NotBlank @Size(max = 255) String insuredFullName,
        @NotNull @Past LocalDate insuredBirthDate,
        @NotBlank @Size(max = 64) String insuredDocumentNumber,
        @NotNull @DecimalMin("10000.00") @DecimalMax("10000000.00") BigDecimal coverageAmount,
        @NotNull @Min(1) @Max(30) Integer termYears) {
}
