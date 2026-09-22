package io.github.dmitrypoverov.insurance.applications;

import io.github.dmitrypoverov.insurance.security.CurrentUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ApplicationSpecifications {

    static Specification<Application> hasId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    private static Specification<Application> ownedBy(String applicantSubject) {
        return (root, query, cb) -> cb.equal(root.get("applicantSubject"), applicantSubject);
    }

    static Specification<Application> visibleTo(CurrentUser user) {
        if (user.isUnderwriter()) {
            return Specification.unrestricted();
        }
        return ownedBy(user.subject());
    }

    static Specification<Application> matches(ApplicationFilter filter) {
        return Specification.allOf(List.of(
                statusIs(filter.status()),
                createdOnOrAfter(filter.createdFrom()),
                createdOnOrBefore(filter.createdTo()),
                coverageAtLeast(filter.coverageFrom()),
                coverageAtMost(filter.coverageTo()),
                ownedByIfGiven(filter.applicantSubject())));
    }

    private static Specification<Application> statusIs(@Nullable ApplicationStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Application> createdOnOrAfter(@Nullable LocalDate day) {
        if (day == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDayUtc(day));
    }

    private static Specification<Application> createdOnOrBefore(@Nullable LocalDate day) {
        if (day == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), startOfDayUtc(day.plusDays(1)));
    }

    private static Specification<Application> coverageAtLeast(@Nullable BigDecimal amount) {
        if (amount == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("coverageAmount"), amount);
    }

    private static Specification<Application> coverageAtMost(@Nullable BigDecimal amount) {
        if (amount == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("coverageAmount"), amount);
    }

    private static Specification<Application> ownedByIfGiven(@Nullable String applicantSubject) {
        if (applicantSubject == null) {
            return Specification.unrestricted();
        }
        return ownedBy(applicantSubject);
    }

    private static Instant startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
