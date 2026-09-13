package io.github.dmitrypoverov.insurance.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SortWhitelist {

    public static void requireAllowed(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new UnsupportedSortPropertyException(order.getProperty(), allowed);
            }
        }
    }
}
