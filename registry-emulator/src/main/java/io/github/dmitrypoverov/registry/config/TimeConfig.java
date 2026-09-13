package io.github.dmitrypoverov.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class TimeConfig {

    private static final Duration POSTGRES_TIMESTAMP_PRECISION = Duration.of(1, ChronoUnit.MICROS);

    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), POSTGRES_TIMESTAMP_PRECISION);
    }
}
