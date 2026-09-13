package io.github.dmitrypoverov.insurance.applications;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Application getOwnById(UUID id, String applicantSubject) {
        return applicationRepository.findByIdAndApplicantSubject(id, applicantSubject)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('UNDERWRITER')")
    public Application getAnyById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }
}