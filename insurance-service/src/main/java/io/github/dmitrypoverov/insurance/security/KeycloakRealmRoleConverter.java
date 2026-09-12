package io.github.dmitrypoverov.insurance.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (realmAccess == null) {
      return List.of();
    }
    if (!(realmAccess.get(ROLES_CLAIM) instanceof Collection<?> roles)) {
      return List.of();
    }
    Collection<GrantedAuthority> authorities = new ArrayList<>(roles.size());
    for (Object role : roles) {
      authorities.add(
          new SimpleGrantedAuthority(ROLE_PREFIX + String.valueOf(role).toUpperCase(Locale.ROOT)));
    }
    return authorities;
  }
}
