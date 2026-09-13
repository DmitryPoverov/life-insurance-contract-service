package io.github.dmitrypoverov.registry.registrations;

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
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false, length = 32)
    private String registryRecordId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID contractId;

    @Column(nullable = false, length = 32)
    private String contractNumber;

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
    private Instant registeredAt;

    public static Registration register(RegistrationRequest request, Instant registeredAt) {
        Registration registration = new Registration();
        registration.contractId = request.contractId();
        registration.contractNumber = request.contractNumber();
        registration.insuredFullName = request.insuredFullName();
        registration.insuredBirthDate = request.insuredBirthDate();
        registration.insuredDocumentNumber = request.insuredDocumentNumber();
        registration.coverageAmount = request.coverageAmount();
        registration.premium = request.premium();
        registration.startDate = request.startDate();
        registration.endDate = request.endDate();
        registration.registeredAt = registeredAt;
        return registration;
    }
}
