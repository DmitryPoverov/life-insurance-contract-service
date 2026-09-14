package io.github.dmitrypoverov.insurance.contracts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
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
            ContractIssueResult result = contractIssuer.issue(applicationId, underwriterSubject);
            if (result.created()) {
                log.info("Issued contract {} for application {}",
                        result.contract().getContractNumber(), applicationId);
            }
            return result;
        } catch (PessimisticLockingFailureException lockTimeout) {
            return contractRepository.findByApplicationId(applicationId)
                    .map(ContractIssueResult::alreadyIssued)
                    .orElseThrow(() -> new ContractIssuanceBusyException(applicationId, lockTimeout));
        }
    }
}
