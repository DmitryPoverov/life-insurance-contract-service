package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.registrations.ContractRegistration;
import io.github.dmitrypoverov.insurance.registrations.RegistrationStatus;
import io.github.dmitrypoverov.insurance.security.CurrentUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContractSpecifications {

    static Specification<Contract> hasId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    private static Specification<Contract> ownedBy(String policyholderSubject) {
        return (root, query, cb) -> cb.equal(root.get("policyholderSubject"), policyholderSubject);
    }

    static Specification<Contract> visibleTo(CurrentUser user) {
        if (user.isUnderwriter()) {
            return Specification.unrestricted();
        }
        return ownedBy(user.subject());
    }

    static Specification<Contract> matches(ContractFilter filter) {
        return Specification.allOf(List.of(
                registrationStatusIs(filter.registrationStatus()),
                issuedOnOrAfter(filter.issuedFrom()),
                issuedOnOrBefore(filter.issuedTo()),
                contractNumberIs(filter.contractNumber())));
    }

    private static Specification<Contract> registrationStatusIs(@Nullable RegistrationStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.exists(registrationWithStatus(root, query, cb, status));
    }

    private static Subquery<UUID> registrationWithStatus(Root<Contract> contract, CriteriaQuery<?> query,
                                                         CriteriaBuilder cb, RegistrationStatus status) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<ContractRegistration> registration = subquery.from(ContractRegistration.class);
        return subquery
                .select(registration.get("contractId"))
                .where(
                        cb.equal(registration.get("contractId"), contract.get("id")),
                        cb.equal(registration.get("status"), status));
    }

    private static Specification<Contract> issuedOnOrAfter(@Nullable LocalDate day) {
        if (day == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issuedAt"), startOfDayUtc(day));
    }

    private static Specification<Contract> issuedOnOrBefore(@Nullable LocalDate day) {
        if (day == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThan(root.get("issuedAt"), startOfDayUtc(day.plusDays(1)));
    }

    private static Specification<Contract> contractNumberIs(@Nullable String contractNumber) {
        if (contractNumber == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("contractNumber"), contractNumber);
    }

    private static Instant startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
