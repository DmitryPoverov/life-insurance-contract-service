package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.applications.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true, updatable = false)
    private Application application;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false, length = 32)
    private String contractNumber;

    @Column(nullable = false, length = 64)
    private String policyholderSubject;

    @Column(nullable = false)
    private String insuredFullName;

    @Column(nullable = false)
    private LocalDate insuredBirthDate;

    @Column(nullable = false, length = 64)
    private String insuredDocumentNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal premium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(nullable = false, length = 64, updatable = false)
    private String issuedBySubject;

    public static Contract issue(Application application, String issuedBySubject, Instant issuedAt,
                                 LocalDate startDate) {
        Contract contract = new Contract();
        contract.application = application;
        contract.policyholderSubject = application.getApplicantSubject();
        contract.insuredFullName = application.getInsuredFullName();
        contract.insuredBirthDate = application.getInsuredBirthDate();
        contract.insuredDocumentNumber = application.getInsuredDocumentNumber();
        contract.coverageAmount = application.getCoverageAmount();
        contract.premium = application.getCalculatedPremium();
        contract.startDate = startDate;
        contract.endDate = startDate.plusYears(application.getTermYears()).minusDays(1);
        contract.issuedAt = issuedAt;
        contract.issuedBySubject = issuedBySubject;
        return contract;
    }
}
