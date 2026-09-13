package io.github.dmitrypoverov.registry.emulator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

public record EmulatorModeRequest(
        @NotNull EmulatorMode mode,
        @Nullable @Positive @Max(120) Integer slowDelaySeconds) {
}
