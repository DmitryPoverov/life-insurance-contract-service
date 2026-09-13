package io.github.dmitrypoverov.registry.emulator;

import java.time.Duration;

public record ActiveMode(EmulatorMode mode, Duration slowDelay) {
}
