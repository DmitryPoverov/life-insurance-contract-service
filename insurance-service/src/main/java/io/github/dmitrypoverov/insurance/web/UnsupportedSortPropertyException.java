package io.github.dmitrypoverov.insurance.web;

import java.util.Set;
import java.util.TreeSet;

public class UnsupportedSortPropertyException extends RuntimeException {

    public UnsupportedSortPropertyException(String property, Set<String> allowed) {
        super("Sorting by '%s' is not supported, allowed: %s"
                .formatted(property, String.join(", ", new TreeSet<>(allowed))));
    }
}
