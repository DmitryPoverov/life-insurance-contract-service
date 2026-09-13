package io.github.dmitrypoverov.insurance.registrations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RegistrationRetryPropertiesTest {

    private final RegistrationRetryProperties retry =
            new RegistrationRetryProperties(Duration.ofSeconds(5), 2, Duration.ofMinutes(5));

    @Test
    void delayAfter_firstAttempt_returnsBaseDelay() {
        assertThat(retry.delayAfter(1)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void delayAfter_laterAttempts_growsByMultiplier() {
        assertThat(retry.delayAfter(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(retry.delayAfter(4)).isEqualTo(Duration.ofSeconds(40));
    }

    @Test
    void delayAfter_manyAttempts_isCappedByMaxDelay() {
        assertThat(retry.delayAfter(10)).isEqualTo(Duration.ofMinutes(5));
        assertThat(retry.delayAfter(10_000)).isEqualTo(Duration.ofMinutes(5));
    }
}
