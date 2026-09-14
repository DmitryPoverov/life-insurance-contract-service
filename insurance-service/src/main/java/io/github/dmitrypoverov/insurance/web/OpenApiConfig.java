package io.github.dmitrypoverov.insurance.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH2_SCHEME = "keycloakPassword";
    private static final String TOKEN_URL = "http://localhost:8080/realms/insurance/protocol/openid-connect/token";

    private static final String DESCRIPTION = """
            Кнопка Authorize ниже открывает форму логина: логин и пароль отправляются \
            напрямую в Keycloak, сервис страхования их не видит.
            """;

    @Bean
    OpenAPI insuranceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Сервис страхования жизни").version("v1").description(DESCRIPTION))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH2_SCHEME))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        OAUTH2_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.OAUTH2)
                                                .flows(new OAuthFlows()
                                                        .password(new OAuthFlow().tokenUrl(TOKEN_URL)))));
    }
}
