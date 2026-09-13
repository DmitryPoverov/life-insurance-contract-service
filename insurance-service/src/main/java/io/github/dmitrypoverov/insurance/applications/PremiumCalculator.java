package io.github.dmitrypoverov.insurance.applications;

import io.github.dmitrypoverov.insurance.applications.PremiumCalculationProperties.AgeFactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Component
@RequiredArgsConstructor
public class PremiumCalculator {

    private static final int MONEY_SCALE = 2;

    private final PremiumCalculationProperties properties;

    public BigDecimal calculate(
            BigDecimal coverageAmount,
            int termYears,
            LocalDate birthDate,
            LocalDate onDate) {
        int age = Period.between(birthDate, onDate).getYears();
        BigDecimal ageFactor =
                properties.ageFactors().stream()
                        .filter(factor -> factor.covers(age))
                        .findFirst()
                        .map(AgeFactor::factor)
                        .orElseThrow(() -> new ApplicantAgeNotEligibleException(age));

        return coverageAmount
                .multiply(properties.baseRate())
                .multiply(ageFactor)
                .multiply(BigDecimal.valueOf(termYears))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
