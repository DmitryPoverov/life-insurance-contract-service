package io.github.dmitrypoverov.insurance.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TestJwtTokens {

    private static final RSAKey KEY = generateKey();
    private static final String ISSUER = "http://localhost:8080/realms/insurance";
    private static final long LIFETIME_SECONDS = 300;

    private TestJwtTokens() {
    }

    public static JwtDecoder decoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot build test JWT decoder", e);
        }
    }

    public static String tokenFor(String subject, String... realmRoles) {
        Instant now = Instant.now();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issuer(ISSUER)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(LIFETIME_SECONDS)))
                        .claim("realm_access", Map.of("roles", List.of(realmRoles)))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
        try {
            jwt.sign(new RSASSASigner(KEY.toPrivateKey()));
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign test token", e);
        }
        return jwt.serialize();
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot generate test RSA key", e);
        }
    }
}
