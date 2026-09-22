package io.github.dmitrypoverov.insurance.applications;

import io.github.dmitrypoverov.insurance.security.CurrentUser;
import io.github.dmitrypoverov.insurance.web.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.hasId;
import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.matches;
import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.visibleTo;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PremiumCalculator premiumCalculator;
    private final Clock clock;

    @Transactional
    public Application create(String applicantSubject,
                              ApplicationCreateRequest request) {

        BigDecimal premium =
                premiumCalculator.calculate(
                        request.coverageAmount(),
                        request.termYears(),
                        request.insuredBirthDate(),
                        LocalDate.now(clock));

        Application application =
                Application.submit(
                        applicantSubject,
                        request.insuredFullName(),
                        request.insuredBirthDate(),
                        request.insuredDocumentNumber(),
                        request.coverageAmount(),
                        request.termYears(),
                        premium);

        return applicationRepository.save(application);
    }

    @Transactional
    public Application approve(UUID id,
                               String underwriterSubject) {

        Application application = lockApplication(id);
        application.approve(underwriterSubject, Instant.now(clock));
        return application;
    }

    @Transactional
    public Application reject(UUID id,
                              String underwriterSubject,
                              String reason) {

        Application application = lockApplication(id);
        application.reject(underwriterSubject, reason, Instant.now(clock));
        return application;
    }

    @Transactional(readOnly = true)
    public Application getById(UUID id,
                               CurrentUser currentUser) {

        return applicationRepository
                .findOne(hasId(id).and(visibleTo(currentUser)))
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Application> find(CurrentUser currentUser,
                                  ApplicationFilter filter,
                                  Pageable pageable) {

        return applicationRepository
                .findAll(visibleTo(currentUser).and(matches(filter)), Paging.withStableOrder(pageable));
    }

    private Application lockApplication(UUID id) {

        return applicationRepository
                .findWithLockById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}
