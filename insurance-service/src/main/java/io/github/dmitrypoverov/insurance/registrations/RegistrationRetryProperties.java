package io.github.dmitrypoverov.insurance.registrations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "insurance.registration.retry")
public record RegistrationRetryProperties(Duration baseDelay, double multiplier, Duration maxDelay) {

    public Duration delayAfter(int attempts) {
        double delayMillis = baseDelay.toMillis() * Math.pow(multiplier, attempts - 1.0);
        return Duration.ofMillis((long) Math.min(delayMillis, maxDelay.toMillis()));
    }
}
