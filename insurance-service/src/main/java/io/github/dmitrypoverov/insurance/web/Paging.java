package io.github.dmitrypoverov.insurance.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Paging {

    // With equal sort values PostgreSQL may return rows in a different order on each page query,
    // so a page could repeat or skip a row. The id as the last key makes the order unambiguous.
    public static Pageable withStableOrder(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }
}
