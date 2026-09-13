package io.github.dmitrypoverov.insurance.registrations;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRegistrationRepository extends JpaRepository<ContractRegistration, UUID> {

    Optional<ContractRegistration> findByContractId(UUID contractId);

    List<ContractRegistration> findByContractIdIn(Collection<UUID> contractIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ContractRegistration> findWithLockById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ContractRegistration> findWithLockByContractId(UUID contractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "org.hibernate.lockMode", value = "upgrade-skiplocked"))
    Optional<ContractRegistration> findFirstByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            RegistrationStatus status, Instant now);
}
