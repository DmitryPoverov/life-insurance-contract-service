package io.github.dmitrypoverov.insurance.registrations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class RegistrationOutcomeRecorder {

    private final ContractRegistrationRepository contractRegistrationRepository;
    private final RegistrationRetryProperties retryProperties;
    private final Clock clock;

    @Transactional
    public void recordRegistered(ContractRegistration claimed, RegistryRecord registryRecord) {
        findIfStillClaimed(claimed).ifPresent(registration -> registration.markRegistered(registryRecord));
    }

    @Transactional
    public void recordRejected(ContractRegistration claimed, String error) {
        findIfStillClaimed(claimed).ifPresent(registration -> registration.markRejected(error));
    }

    @Transactional
    public void recordFailed(ContractRegistration claimed, String error) {
        findIfStillClaimed(claimed).ifPresent(registration -> registration.markFailed(error));
    }

    @Transactional
    public void recordRetry(ContractRegistration claimed, String error) {
        Instant retryAt = Instant.now(clock).plus(retryProperties.delayAfter(claimed.getAttempts()));
        findIfStillClaimed(claimed).ifPresent(registration -> registration.scheduleRetry(error, retryAt));
    }

    private Optional<ContractRegistration> findIfStillClaimed(ContractRegistration claimed) {
        ContractRegistration current = contractRegistrationRepository.findWithLockById(claimed.getId())
                .orElseThrow();
        if (current.getAttempts() != claimed.getAttempts()) {
            log.warn("Registration {} was claimed again (attempt {} instead of {}), outcome discarded",
                    current.getId(), current.getAttempts(), claimed.getAttempts());
            return Optional.empty();
        }
        return Optional.of(current);
    }
}
