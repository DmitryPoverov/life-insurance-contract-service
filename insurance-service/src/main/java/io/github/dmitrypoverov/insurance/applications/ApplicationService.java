package io.github.dmitrypoverov.insurance.applications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.hasId;
import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.matches;
import static io.github.dmitrypoverov.insurance.applications.ApplicationSpecifications.ownedBy;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PremiumCalculator premiumCalculator;
    private final Clock clock;

    @Transactional
    public Application create(String applicantSubject, ApplicationCreateRequest request) {
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
    public Application approve(UUID id, String underwriterSubject) {
        Application application = lockApplication(id);
        application.approve(underwriterSubject, Instant.now(clock));
        return application;
    }

    @Transactional
    public Application reject(UUID id, String underwriterSubject, String reason) {
        Application application = lockApplication(id);
        application.reject(underwriterSubject, reason, Instant.now(clock));
        return application;
    }

    @Transactional(readOnly = true)
    public Application getOwnById(UUID id, String applicantSubject) {
        return applicationRepository.findOne(hasId(id).and(ownedBy(applicantSubject)))
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('UNDERWRITER')")
    public Application getAnyById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Application> findOwn(String applicantSubject, ApplicationFilter filter, Pageable pageable) {
        return applicationRepository.findAll(
                ownedBy(applicantSubject).and(matches(filter)), withStableOrder(pageable));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('UNDERWRITER')")
    public Page<Application> findAny(ApplicationFilter filter, Pageable pageable) {
        return applicationRepository.findAll(matches(filter), withStableOrder(pageable));
    }

    private Application lockApplication(UUID id) {
        return applicationRepository.findWithLockById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private static Pageable withStableOrder(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }
}
