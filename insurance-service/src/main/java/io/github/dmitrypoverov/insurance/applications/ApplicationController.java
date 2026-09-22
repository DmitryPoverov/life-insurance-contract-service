package io.github.dmitrypoverov.insurance.applications;

import io.github.dmitrypoverov.insurance.security.Roles;
import io.github.dmitrypoverov.insurance.web.SortWhitelist;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "coverageAmount");

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationCreateRequest request,
                                               Authentication authentication) {

        Application application = applicationService.create(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + application.getId()))
                .body(applicationMapper.toResponse(application));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'UNDERWRITER')")
    PagedModel<ApplicationResponse> list(@ParameterObject ApplicationFilter filter,
                                         @ParameterObject
                                         @PageableDefault(size = 20,
                                                 sort = "createdAt",
                                                 direction = Sort.Direction.DESC)
                                         Pageable pageable,
                                         Authentication authentication) {

        SortWhitelist.requireAllowed(pageable, SORTABLE_PROPERTIES);
        Page<Application> page = Roles.isUnderwriter(authentication)
                ? applicationService.findAny(filter, pageable)
                : applicationService.findOwn(authentication.getName(), filter, pageable);
        return new PagedModel<>(page.map(applicationMapper::toResponse));
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'UNDERWRITER')")
    ResponseEntity<ApplicationResponse> getById(@PathVariable UUID applicationId,
                                                Authentication authentication) {

        Application application = Roles.isUnderwriter(authentication)
                ? applicationService.getAnyById(applicationId)
                : applicationService.getOwnById(applicationId, authentication.getName());
        return ResponseEntity.ok(applicationMapper.toResponse(application));
    }

    @PostMapping("/{applicationId}/approve")
    @PreAuthorize("hasRole('UNDERWRITER')")
    ApplicationResponse approve(@PathVariable UUID applicationId,
                                Authentication authentication) {

        return applicationMapper.toResponse(applicationService.approve(applicationId, authentication.getName()));
    }

    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasRole('UNDERWRITER')")
    ApplicationResponse reject(@PathVariable UUID applicationId,
                               @Valid @RequestBody ApplicationRejectRequest request,
                               Authentication authentication) {

        return applicationMapper.toResponse(
                applicationService.reject(applicationId, authentication.getName(), request.reason()));
    }
}
