package io.github.dmitrypoverov.insurance.registrations;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/v1/registrations")
public interface RegistryApi {

    @PostExchange
    @Nullable RegistryRegistrationResponse register(@RequestBody RegistryRegistrationRequest request);
}
