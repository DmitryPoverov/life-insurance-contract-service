package io.github.dmitrypoverov.insurance.contracts;

import io.github.dmitrypoverov.insurance.security.Roles;
import io.github.dmitrypoverov.insurance.web.SortWhitelist;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContractController {

    private static final Set<String> SORTABLE_PROPERTIES = Set.of("issuedAt", "contractNumber");

    private final ContractIssuanceService contractIssuanceService;
    private final ContractService contractService;
    private final ContractMapper contractMapper;

    @PostMapping("/api/v1/applications/{applicationId}/contract")
    @PreAuthorize("hasRole('UNDERWRITER')")
    ResponseEntity<ContractResponse> issue(@PathVariable UUID applicationId, Authentication authentication) {
        ContractIssueResult result = contractIssuanceService.issue(applicationId, authentication.getName());
        ContractResponse body = contractMapper.toResponse(contractService.detailsOf(result.contract()));

        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/contracts/" + body.id())).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/v1/contracts/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'UNDERWRITER')")
    ContractResponse getById(@PathVariable UUID id, Authentication authentication) {
        ContractDetails details = Roles.isUnderwriter(authentication)
                ? contractService.getAnyById(id)
                : contractService.getOwnById(id, authentication.getName());
        return contractMapper.toResponse(details);
    }

    @GetMapping("/api/v1/contracts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'UNDERWRITER')")
    PagedModel<ContractResponse> list(
            @ParameterObject ContractFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {

        SortWhitelist.requireAllowed(pageable, SORTABLE_PROPERTIES);
        Page<ContractDetails> page = Roles.isUnderwriter(authentication)
                ? contractService.findAny(filter, pageable)
                : contractService.findOwn(authentication.getName(), filter, pageable);
        return new PagedModel<>(page.map(contractMapper::toResponse));
    }
}
