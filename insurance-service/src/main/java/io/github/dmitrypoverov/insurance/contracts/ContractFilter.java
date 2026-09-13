package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ContractFilter(
        @Nullable RegistrationStatus registrationStatus,
        @Parameter(description = "Issue day in UTC, inclusive, e.g. 2026-09-13")
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedFrom,
        @Parameter(description = "Issue day in UTC, inclusive, e.g. 2026-09-13")
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedTo,
        @Nullable String contractNumber) {
}
