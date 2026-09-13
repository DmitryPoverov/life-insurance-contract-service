package io.github.dmitrypoverov.registry.registrations;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final RegistrationMapper registrationMapper;

    @PostMapping
    ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResult result = registrationService.register(request);
        RegistrationResponse body = registrationMapper.toResponse(result.registration());

        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/registrations/" + body.contractId())).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{contractId}")
    RegistrationResponse getByContractId(@PathVariable UUID contractId) {
        return registrationMapper.toResponse(registrationService.getByContractId(contractId));
    }

    @GetMapping
    PagedModel<RegistrationResponse> list(@RequestParam(required = false) @Nullable UUID contractId,
                                          Pageable pageable) {
        return new PagedModel<>(registrationService.find(contractId, pageable).map(registrationMapper::toResponse));
    }
}
