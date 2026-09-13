package io.github.dmitrypoverov.insurance.registrations;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ContractRegistrationClaimer {

    private final ContractRegistrationRepository contractRegistrationRepository;
    private final RegistrationSchedulerProperties schedulerProperties;
    private final Clock clock;

    @Transactional
    public Optional<ContractRegistration> claimNext() {
        Instant now = Instant.now(clock);
        Optional<ContractRegistration> due = contractRegistrationRepository
                .findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(RegistrationStatus.PENDING, now);
        if (due.isPresent()) {
            due.get().startAttempt(now.plus(schedulerProperties.leaseDuration()));
        }
        return due;
    }
}
