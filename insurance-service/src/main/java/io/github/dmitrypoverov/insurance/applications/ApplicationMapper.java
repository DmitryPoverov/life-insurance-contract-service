package io.github.dmitrypoverov.insurance.applications;

import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getApplicantSubject(),
                application.getInsuredFullName(),
                application.getInsuredBirthDate(),
                application.getInsuredDocumentNumber(),
                application.getCoverageAmount(),
                application.getTermYears(),
                application.getCalculatedPremium(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getDecidedAt(),
                application.getDecidedBySubject(),
                application.getRejectionReason());
    }
}
