package io.github.dmitrypoverov.insurance.registrations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContractRegistrationRepository extends JpaRepository<ContractRegistration, UUID> {
}
