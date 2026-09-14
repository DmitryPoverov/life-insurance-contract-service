package io.github.dmitrypoverov.insurance.config;

import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {

    // HTTP-клиент JDK по умолчанию предлагает серверу перейти на HTTP/2, и не каждый сервер доводит такой POST до ответа.
    @Bean
    ClientHttpRequestFactoryBuilderCustomizer<JdkClientHttpRequestFactoryBuilder> http11OnlyClient() {
        return builder -> builder.withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1));
    }
}
