package io.github.dmitrypoverov.insurance.registrations;

import io.github.dmitrypoverov.insurance.contracts.Contract;
import io.github.dmitrypoverov.insurance.contracts.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractRegistrationSender {

    private final ContractRepository contractRepository;
    private final RegistryClient registryClient;
    private final RegistrationOutcomeRecorder outcomeRecorder;

    public void send(ContractRegistration claimed) {
        RegistryRecord registryRecord;
        try {
            Contract contract = contractRepository.findById(claimed.getContractId()).orElseThrow();
            registryRecord = registryClient.register(contract);
        } catch (RegistryRejectedException rejection) {
            outcomeRecorder.recordRejected(claimed, describe(rejection));
            return;
        } catch (PermanentRegistryException failure) {
            outcomeRecorder.recordFailed(claimed, describe(failure));
            return;
        } catch (Exception retryableOrUnexpected) {
            outcomeRecorder.recordRetry(claimed, describe(retryableOrUnexpected));
            return;
        }

        outcomeRecorder.recordRegistered(claimed, registryRecord);
    }

    private static String describe(Exception error) {
        String message = error.getMessage();
        return message != null ? message : error.getClass().getName();
    }
}
