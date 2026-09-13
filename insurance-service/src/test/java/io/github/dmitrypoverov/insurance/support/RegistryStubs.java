package io.github.dmitrypoverov.insurance.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class RegistryStubs {

    public static final String REGISTRY_RECORD_ID = "GSR-2026-000001";
    public static final String REGISTERED_AT = "2026-09-13T19:24:58.699218Z";

    private static final String REGISTRATIONS_PATH = "/api/v1/registrations";

    private RegistryStubs() {
    }

    public static void respondWithRegistration(WireMockServer registry, int status) {
        respond(registry, aResponse()
                .withStatus(status)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(resource("registry/registration-response.json")));
    }

    public static void respondWithRejection(WireMockServer registry) {
        respond(registry, aResponse()
                .withStatus(422)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .withBody(resource("registry/registration-rejected.json")));
    }

    public static void respond(WireMockServer registry, ResponseDefinitionBuilder response) {
        registry.stubFor(post(urlEqualTo(REGISTRATIONS_PATH)).willReturn(response));
    }

    private static String resource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
