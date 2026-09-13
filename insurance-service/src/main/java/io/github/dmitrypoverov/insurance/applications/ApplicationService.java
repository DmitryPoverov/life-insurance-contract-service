package io.github.dmitrypoverov.insurance.applications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

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
}
