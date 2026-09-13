package io.github.dmitrypoverov.insurance.applications;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository
    extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {}
