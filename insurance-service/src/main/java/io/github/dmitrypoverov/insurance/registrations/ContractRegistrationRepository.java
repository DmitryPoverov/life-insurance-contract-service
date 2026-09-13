package io.github.dmitrypoverov.insurance.registrations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRegistrationRepository extends JpaRepository<ContractRegistration, UUID> {

    Optional<ContractRegistration> findByContractId(UUID contractId);

    List<ContractRegistration> findByContractIdIn(Collection<UUID> contractIds);
}
