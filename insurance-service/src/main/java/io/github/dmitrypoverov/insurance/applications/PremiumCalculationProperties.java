package io.github.dmitrypoverov.insurance.applications;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "insurance.premium")
public record PremiumCalculationProperties(BigDecimal baseRate, List<AgeFactor> ageFactors) {

    public record AgeFactor(int fromAge, int toAge, BigDecimal factor) {

        public boolean covers(int age) {
            return age >= fromAge && age <= toAge;
        }
    }
}
