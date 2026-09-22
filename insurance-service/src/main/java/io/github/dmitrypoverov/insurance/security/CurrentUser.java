package io.github.dmitrypoverov.insurance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Objects;

public record CurrentUser(String subject, boolean isUnderwriter) {

    private static final String UNDERWRITER_AUTHORITY = "ROLE_UNDERWRITER";

    private static boolean hasUnderwriterRole(Authentication authentication) {
        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> UNDERWRITER_AUTHORITY.equals(authority.getAuthority()));
    }

    static CurrentUser from(JwtAuthenticationToken authentication) {
        String subjectFromAuth = authentication.getToken().getSubject();
        String subject = Objects.requireNonNull(subjectFromAuth, "JWT has no 'sub' claim");
        return new CurrentUser(subject, hasUnderwriterRole(authentication));
    }
}
