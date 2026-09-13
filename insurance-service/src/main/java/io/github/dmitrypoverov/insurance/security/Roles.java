package io.github.dmitrypoverov.insurance.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Roles {

    private static final String UNDERWRITER_AUTHORITY = "ROLE_UNDERWRITER";

    public static boolean isUnderwriter(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> UNDERWRITER_AUTHORITY.equals(authority.getAuthority()));
    }
}
