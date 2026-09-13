package io.github.dmitrypoverov.insurance.applications;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String applicantSubject;

    @Column(nullable = false)
    private String insuredFullName;

    @Column(nullable = false)
    private LocalDate insuredBirthDate;

    @Column(nullable = false, length = 64)
    private String insuredDocumentNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Column(nullable = false)
    private int termYears;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal calculatedPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant decidedAt;

    @Column(length = 64)
    private String decidedBySubject;

    @Column(length = 500)
    private String rejectionReason;

    public static Application submit(
            String applicantSubject,
            String insuredFullName,
            LocalDate insuredBirthDate,
            String insuredDocumentNumber,
            BigDecimal coverageAmount,
            int termYears,
            BigDecimal calculatedPremium) {

        Application application = new Application();
        application.applicantSubject = applicantSubject;
        application.insuredFullName = insuredFullName;
        application.insuredBirthDate = insuredBirthDate;
        application.insuredDocumentNumber = insuredDocumentNumber;
        application.coverageAmount = coverageAmount;
        application.termYears = termYears;
        application.calculatedPremium = calculatedPremium;
        application.status = ApplicationStatus.SUBMITTED;

        return application;
    }

    public void approve(String underwriterSubject) {
        changeStatus(ApplicationStatus.APPROVED);
        decidedBySubject = underwriterSubject;
        decidedAt = Instant.now();
    }

    public void reject(String underwriterSubject, String reason) {
        changeStatus(ApplicationStatus.REJECTED);
        decidedBySubject = underwriterSubject;
        decidedAt = Instant.now();
        rejectionReason = reason;
    }

    public void issueContract() {
        changeStatus(ApplicationStatus.CONTRACT_ISSUED);
    }

    private void changeStatus(ApplicationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ApplicationStatusTransitionException(id, status, target);
        }
        status = target;
    }
}

