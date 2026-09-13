package io.github.dmitrypoverov.insurance.contracts;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractIssuanceService {

    private final ContractRepository contractRepository;
    private final ContractIssuer contractIssuer;

    public ContractIssueResult issue(UUID applicationId, String underwriterSubject) {
        Optional<Contract> existing = contractRepository.findByApplicationId(applicationId);
        if (existing.isPresent()) {
            return ContractIssueResult.alreadyIssued(existing.get());
        }

        try {
            return contractIssuer.issue(applicationId, underwriterSubject);
        } catch (PessimisticLockingFailureException lockTimeout) {
            return contractRepository.findByApplicationId(applicationId)
                    .map(ContractIssueResult::alreadyIssued)
                    .orElseThrow(() -> new ContractIssuanceBusyException(applicationId, lockTimeout));
        }
    }
}
