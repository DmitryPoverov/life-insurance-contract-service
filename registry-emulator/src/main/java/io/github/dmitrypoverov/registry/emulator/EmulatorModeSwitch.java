package io.github.dmitrypoverov.registry.emulator;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class EmulatorModeSwitch {

    private final Duration defaultSlowDelay;
    private final AtomicReference<ActiveMode> activeMode;

    public EmulatorModeSwitch(@Value("${registry.emulator.default-slow-delay}") Duration defaultSlowDelay) {
        this.defaultSlowDelay = defaultSlowDelay;
        this.activeMode = new AtomicReference<>(new ActiveMode(EmulatorMode.SUCCESS, defaultSlowDelay));
    }

    public ActiveMode current() {
        return activeMode.get();
    }

    public ActiveMode switchTo(EmulatorMode mode, @Nullable Duration slowDelay) {
        ActiveMode switched = new ActiveMode(mode, slowDelay != null ? slowDelay : defaultSlowDelay);
        ActiveMode previous = activeMode.getAndSet(switched);
        log.info("Emulator mode switched from {} to {}", previous.mode(), mode);
        return switched;
    }
}
