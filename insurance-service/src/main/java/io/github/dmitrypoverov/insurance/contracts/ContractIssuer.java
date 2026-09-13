package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.applications.Application;
import io.github.dmitrypoverov.insurance.applications.ApplicationNotFoundException;
import io.github.dmitrypoverov.insurance.applications.ApplicationRepository;
import io.github.dmitrypoverov.insurance.applications.ApplicationStatus;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ContractIssuer {

    private final ApplicationRepository applicationRepository;
    private final ContractRepository contractRepository;
    private final ContractRegistrationRepository contractRegistrationRepository;
    private final Clock clock;

    @Transactional
    public ContractIssueResult issue(UUID applicationId, String underwriterSubject) {
        Application application = applicationRepository.findWithLockById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (application.getStatus() == ApplicationStatus.CONTRACT_ISSUED) {
            return ContractIssueResult.alreadyIssued(findExistingContract(applicationId));
        }

        application.issueContract();

        Instant now = Instant.now(clock);
        LocalDate startDate = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Contract contract = contractRepository.save(
                Contract.issue(application, underwriterSubject, now, startDate));
        contractRegistrationRepository.save(
                ContractRegistration.pending(contract.getId(), now, null));

        return ContractIssueResult.newlyIssued(contract);
    }

    private Contract findExistingContract(UUID applicationId) {
        return contractRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Application %s has status CONTRACT_ISSUED but no contract".formatted(applicationId)));
    }
}
