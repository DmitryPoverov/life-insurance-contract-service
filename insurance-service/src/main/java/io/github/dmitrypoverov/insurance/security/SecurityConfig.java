package io.github.dmitrypoverov.insurance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                                            ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        // CSRF не нужен, т.к. используем bearer-токен в заголовке Authorization, его ставит только код
        // клиента, браузер сам такой заголовок не добавляет.
        // В цепочке фильтров используется SessionCreationPolicy.STATELESS, поэтому сессий и cookie нет.
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        requests ->
                                requests
                                        .requestMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exceptionHandling ->
                                exceptionHandling
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                // oauth2ResourceServer регистрирует свою точку входа для сбоёв
                                // bearer-токена и без явного переопределения забивает ту, что задана выше.
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
