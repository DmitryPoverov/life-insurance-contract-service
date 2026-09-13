package io.github.dmitrypoverov.insurance.registrations;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class RegistrationScheduler {

    private final ContractRegistrationClaimer claimer;
    private final ContractRegistrationSender sender;
    private final RegistrationSchedulerProperties schedulerProperties;

    @Scheduled(
            initialDelayString = "${insurance.registration.scheduler.poll-interval}",
            fixedDelayString = "${insurance.registration.scheduler.poll-interval}")
    public void processDueRegistrations() {
        for (int i = 0; i < schedulerProperties.maxTasksPerTick(); i++) {
            Optional<ContractRegistration> claimed = claimer.claimNext();
            if (claimed.isEmpty()) {
                return;
            }
            sender.send(claimed.get());
        }
    }
}
