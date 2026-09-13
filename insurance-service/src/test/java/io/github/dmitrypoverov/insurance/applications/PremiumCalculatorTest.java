package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.dmitrypoverov.insurance.applications.PremiumCalculationProperties.AgeFactor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PremiumCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);

    private final PremiumCalculator calculator = new PremiumCalculator(
            new PremiumCalculationProperties(
                    new BigDecimal("0.005"),
                    List.of(
                            new AgeFactor(18, 30, new BigDecimal("1.0")),
                            new AgeFactor(31, 45, new BigDecimal("1.3")))));

    @Test
    void calculate_ageInSecondRange_appliesMatchingFactor() {
        BigDecimal premium = calculator.calculate(
                new BigDecimal("1000000.00"), 10, LocalDate.of(1990, 5, 20), TODAY);

        assertThat(premium).isEqualByComparingTo("65000.00");
    }

    @Test
    void calculate_anyInput_roundsToTwoDecimals() {
        BigDecimal premium = calculator.calculate(
                new BigDecimal("123456.78"), 3, LocalDate.of(2000, 1, 1), TODAY);

        assertThat(premium.scale()).isEqualTo(2);
    }

    @Test
    void calculate_ageOutsideAllRanges_throwsApplicantAgeNotEligible() {
        BigDecimal coverageAmount = new BigDecimal("50000.00");
        LocalDate birthDate = LocalDate.of(1940, 1, 1);

        assertThatThrownBy(() -> calculator.calculate(
                coverageAmount, 5, birthDate, TODAY))
                .isInstanceOf(ApplicantAgeNotEligibleException.class)
                .hasMessageContaining("86");
    }
}
