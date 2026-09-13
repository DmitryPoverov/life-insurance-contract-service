package io.github.dmitrypoverov.registry.emulator;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

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
        activeMode.set(switched);
        return switched;
    }
}
