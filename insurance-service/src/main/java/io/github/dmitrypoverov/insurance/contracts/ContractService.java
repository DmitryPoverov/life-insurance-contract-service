package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.ContractRegistrationRepository;
import io.github.dmitrypoverov.insurance.web.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.dmitrypoverov.insurance.contracts.ContractSpecifications.hasId;
import static io.github.dmitrypoverov.insurance.contracts.ContractSpecifications.matches;
import static io.github.dmitrypoverov.insurance.contracts.ContractSpecifications.ownedBy;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractRegistrationRepository contractRegistrationRepository;

    @Transactional(readOnly = true)
    public ContractDetails getOwnById(UUID id, String policyholderSubject) {
        Contract contract = contractRepository.findOne(hasId(id).and(ownedBy(policyholderSubject)))
                .orElseThrow(() -> new ContractNotFoundException(id));
        return withRegistration(contract);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ContractDetails getAnyById(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));
        return withRegistration(contract);
    }

    @Transactional(readOnly = true)
    public Page<ContractDetails> findOwn(String policyholderSubject, ContractFilter filter, Pageable pageable) {
        Page<Contract> contracts = contractRepository.findAll(
                ownedBy(policyholderSubject).and(matches(filter)), Paging.withStableOrder(pageable));
        return withRegistrations(contracts);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('UNDERWRITER')")
    public Page<ContractDetails> findAny(ContractFilter filter, Pageable pageable) {
        Page<Contract> contracts = contractRepository.findAll(matches(filter), Paging.withStableOrder(pageable));
        return withRegistrations(contracts);
    }

    @Transactional(readOnly = true)
    public ContractDetails detailsOf(Contract contract) {
        return withRegistration(contract);
    }

    private ContractDetails withRegistration(Contract contract) {
        ContractRegistration registration = contractRegistrationRepository.findByContractId(contract.getId())
                .orElseThrow(() -> missingRegistration(contract.getId()));
        return new ContractDetails(contract, registration);
    }

    // One query for the registrations of the whole page instead of one query per contract.
    private Page<ContractDetails> withRegistrations(Page<Contract> contracts) {
        List<UUID> contractIds = contracts.map(Contract::getId).toList();
        Map<UUID, ContractRegistration> registrationsByContractId = contractRegistrationRepository
                .findByContractIdIn(contractIds).stream()
                .collect(Collectors.toMap(ContractRegistration::getContractId, Function.identity()));

        return contracts.map(contract -> new ContractDetails(
                contract, registrationOf(contract.getId(), registrationsByContractId)));
    }

    private static ContractRegistration registrationOf(UUID contractId,
                                                       Map<UUID, ContractRegistration> registrationsByContractId) {
        ContractRegistration registration = registrationsByContractId.get(contractId);
        if (registration == null) {
            throw missingRegistration(contractId);
        }
        return registration;
    }

    private static IllegalStateException missingRegistration(UUID contractId) {
        return new IllegalStateException("Contract %s has no registration record".formatted(contractId));
    }
}
