package io.github.dmitrypoverov.insurance.applications;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository
        extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Application> findWithLockById(UUID id);
}
