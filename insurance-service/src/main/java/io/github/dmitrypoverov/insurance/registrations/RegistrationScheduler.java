package io.github.dmitrypoverov.insurance.registrations;

import io.github.dmitrypoverov.insurance.web.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
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
            sendWithCorrelation(claimed.get());
        }
    }

    private void sendWithCorrelation(ContractRegistration claimed) {
        String requestId = claimed.getRequestId();
        if (requestId != null) {
            MDC.put(CorrelationIdFilter.REQUEST_ID_MDC_KEY, requestId);
        }
        try {
            sender.send(claimed);
        } finally {
            MDC.remove(CorrelationIdFilter.REQUEST_ID_MDC_KEY);
        }
    }
}
