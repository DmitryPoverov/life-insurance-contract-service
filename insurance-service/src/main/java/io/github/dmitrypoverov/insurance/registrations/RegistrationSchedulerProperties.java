package io.github.dmitrypoverov.insurance.registrations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "insurance.registration.scheduler")
public record RegistrationSchedulerProperties(Duration leaseDuration, int maxTasksPerTick) {
}
