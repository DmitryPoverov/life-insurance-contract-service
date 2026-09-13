package io.github.dmitrypoverov.registry.registrations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    Optional<Registration> findByContractId(UUID contractId);

    Page<Registration> findAllByContractId(UUID contractId, Pageable pageable);
}
