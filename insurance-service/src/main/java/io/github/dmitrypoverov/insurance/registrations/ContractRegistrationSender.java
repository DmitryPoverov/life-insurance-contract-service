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
        String contractNumber = null;
        RegistryRecord registryRecord;
        try {
            Contract contract = contractRepository.findById(claimed.getContractId()).orElseThrow();
            contractNumber = contract.getContractNumber();
            registryRecord = registryClient.register(contract);
        } catch (RegistryRejectedException rejection) {
            outcomeRecorder.recordRejected(claimed, contractNumber, describe(rejection));
            return;
        } catch (PermanentRegistryException failure) {
            outcomeRecorder.recordFailed(claimed, contractNumber, describe(failure));
            return;
        } catch (Exception retryableOrUnexpected) {
            outcomeRecorder.recordRetry(claimed, contractNumber, describe(retryableOrUnexpected));
            return;
        }

        outcomeRecorder.recordRegistered(claimed, registryRecord);
    }

    private static String describe(Exception error) {
        String message = error.getMessage();
        return message != null ? message : error.getClass().getName();
    }
}
