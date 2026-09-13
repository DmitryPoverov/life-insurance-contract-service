package io.github.dmitrypoverov.insurance.applications;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationFilter(
        @Nullable ApplicationStatus status,
        @Parameter(description = "Creation day in UTC, inclusive, e.g. 2026-09-13")
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @Parameter(description = "Creation day in UTC, inclusive, e.g. 2026-09-13")
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
        @Nullable BigDecimal coverageFrom,
        @Nullable BigDecimal coverageTo,
        @Nullable String applicantSubject) {
}
