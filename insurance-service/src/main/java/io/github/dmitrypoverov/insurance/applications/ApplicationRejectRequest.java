package io.github.dmitrypoverov.insurance.applications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationRejectRequest(
        @NotBlank @Size(max = 500) String reason) {
}
