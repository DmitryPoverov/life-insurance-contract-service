package io.github.dmitrypoverov.insurance.applications;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    ResponseEntity<ApplicationResponse> create(
            @Valid @RequestBody ApplicationCreateRequest request,
            Authentication authentication) {

        Application application = applicationService.create(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + application.getId()))
                .body(applicationMapper.toResponse(application));
    }
}
