package io.github.dmitrypoverov.insurance.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

// TODO: Удалить после появления эндпоинтов заявок.
@RestController
@RequestMapping("/api/v1")
public class CurrentUserController {

    @GetMapping("/me")
    CurrentUserResponse currentUser(Authentication authentication) {
        List<String> roles =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .sorted()
                        .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }

    record CurrentUserResponse(String sub, List<String> roles) {
    }
}
