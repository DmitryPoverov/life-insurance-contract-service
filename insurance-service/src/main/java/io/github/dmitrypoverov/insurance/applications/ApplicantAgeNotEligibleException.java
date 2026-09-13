package io.github.dmitrypoverov.insurance.applications;

public class ApplicantAgeNotEligibleException extends RuntimeException {

    public ApplicantAgeNotEligibleException(int age) {
        super("Applicant age %d is outside the insurable range".formatted(age));
    }
}
