package io.github.dmitrypoverov.insurance.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH2_SCHEME = "keycloakPassword";
    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String TOKEN_PATH = "/protocol/openid-connect/token";

    private static final String DESCRIPTION = """
            Кнопка Authorize ниже открывает форму логина: логин и пароль отправляются \
            напрямую в Keycloak, сервис страхования их не видит.

            Уже есть токен (например, получен через curl) — можно вставить его напрямую \
            в схему bearerAuth в том же окне, минуя форму логина.
            """;

    @Bean
    OpenAPI insuranceOpenApi(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return new OpenAPI()
                .info(new Info().title("Сервис страхования жизни").version("v1").description(DESCRIPTION))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH2_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        OAUTH2_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.OAUTH2)
                                                .flows(new OAuthFlows()
                                                        .password(new OAuthFlow()
                                                                .tokenUrl(issuerUri + TOKEN_PATH))))
                                .addSecuritySchemes(
                                        BEARER_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
