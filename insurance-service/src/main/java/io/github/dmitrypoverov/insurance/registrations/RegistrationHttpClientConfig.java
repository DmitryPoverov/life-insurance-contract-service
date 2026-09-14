package io.github.dmitrypoverov.insurance.registrations;

import io.github.dmitrypoverov.insurance.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration
class RegistrationHttpClientConfig {

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
