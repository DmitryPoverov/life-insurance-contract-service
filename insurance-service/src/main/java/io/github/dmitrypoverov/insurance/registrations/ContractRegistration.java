package io.github.dmitrypoverov.insurance.registrations;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ContractRegistration {

    private static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RegistrationStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(columnDefinition = "text")
    private @Nullable String lastError;

    @Column(length = 64)
    private @Nullable String registryRecordId;

    private @Nullable Instant registeredAt;

    @Column(length = 64, updatable = false)
    private @Nullable String requestId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public static ContractRegistration pending(UUID contractId, Instant now, @Nullable String requestId) {
        ContractRegistration registration = new ContractRegistration();
        registration.contractId = contractId;
        registration.status = RegistrationStatus.PENDING;
        registration.attempts = 0;
        registration.nextAttemptAt = now;
        registration.requestId = requestId;
        return registration;
    }

    public void startAttempt(Instant leaseExpiresAt) {
        requirePending();
        attempts++;
        nextAttemptAt = leaseExpiresAt;
    }

    public void markRegistered(RegistryRecord registryRecord) {
        requirePending();
        status = RegistrationStatus.REGISTERED;
        registryRecordId = registryRecord.registryRecordId();
        registeredAt = registryRecord.registeredAt();
    }

    public void markRejected(String error) {
        requirePending();
        status = RegistrationStatus.REJECTED;
        lastError = shorten(error);
    }

    public void markFailed(String error) {
        requirePending();
        status = RegistrationStatus.FAILED;
        lastError = shorten(error);
    }

    public void scheduleRetry(String error, Instant retryAt) {
        requirePending();
        lastError = shorten(error);
        nextAttemptAt = retryAt;
    }

    private void requirePending() {
        if (status != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Registration %s is %s, not PENDING".formatted(id, status));
        }
    }

    private static String shorten(String error) {
        return error.length() > LAST_ERROR_MAX_LENGTH ? error.substring(0, LAST_ERROR_MAX_LENGTH) : error;
    }
}
