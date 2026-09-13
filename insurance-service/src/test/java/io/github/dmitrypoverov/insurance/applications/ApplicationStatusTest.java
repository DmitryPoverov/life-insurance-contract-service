package io.github.dmitrypoverov.insurance.applications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApplicationStatusTest {

    @ParameterizedTest
    @CsvSource({
            "SUBMITTED, SUBMITTED, false",
            "SUBMITTED, APPROVED, true",
            "SUBMITTED, REJECTED, true",
            "SUBMITTED, CONTRACT_ISSUED, false",
            "APPROVED, SUBMITTED, false",
            "APPROVED, APPROVED, false",
            "APPROVED, REJECTED, false",
            "APPROVED, CONTRACT_ISSUED, true",
            "REJECTED, SUBMITTED, false",
            "REJECTED, APPROVED, false",
            "REJECTED, REJECTED, false",
            "REJECTED, CONTRACT_ISSUED, false",
            "CONTRACT_ISSUED, SUBMITTED, false",
            "CONTRACT_ISSUED, APPROVED, false",
            "CONTRACT_ISSUED, REJECTED, false",
            "CONTRACT_ISSUED, CONTRACT_ISSUED, false"
    })
    void canTransitionTo_eachPair_matchesLifecycle(
            ApplicationStatus from, ApplicationStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}
