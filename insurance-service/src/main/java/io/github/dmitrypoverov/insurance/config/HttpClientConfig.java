package io.github.dmitrypoverov.insurance.config;

import io.github.dmitrypoverov.insurance.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {

    // HTTP-клиент JDK по умолчанию предлагает серверу перейти на HTTP/2, и не каждый сервер доводит такой POST до ответа.
    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<JdkClientHttpRequestFactoryBuilder> http11OnlyClient() {
        return builder -> builder.withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1));
    }

    @Bean
    RestClientHttpServiceGroupConfigurer correlationIdPropagation() {
        return groups -> groups.filterByName("registry").forEachClient((group, builder) ->
                builder.requestInterceptor((request, body, execution) -> {
                    String requestId = MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY);
                    if (requestId != null) {
                        request.getHeaders().add(CorrelationIdFilter.REQUEST_ID_HEADER, requestId);
                    }
                    return execution.execute(request, body);
                }));
    }
}
