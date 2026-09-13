package io.github.dmitrypoverov.registry.emulator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emulator/mode")
public class EmulatorModeController {

    private final EmulatorModeSwitch emulatorModeSwitch;

    @GetMapping
    EmulatorModeResponse current() {
        return toResponse(emulatorModeSwitch.current());
    }

    @PostMapping
    EmulatorModeResponse switchMode(@Valid @RequestBody EmulatorModeRequest request) {
        Duration slowDelay = request.slowDelaySeconds() != null
                ? Duration.ofSeconds(request.slowDelaySeconds())
                : null;
        return toResponse(emulatorModeSwitch.switchTo(request.mode(), slowDelay));
    }

    private EmulatorModeResponse toResponse(ActiveMode activeMode) {
        return new EmulatorModeResponse(activeMode.mode(), activeMode.slowDelay().toSeconds());
    }
}
