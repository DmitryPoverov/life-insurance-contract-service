package io.github.dmitrypoverov.insurance.contracts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContractController {

    private final ContractIssuanceService contractIssuanceService;
    private final ContractMapper contractMapper;

    @PostMapping("/api/v1/applications/{applicationId}/contract")
    @PreAuthorize("hasRole('UNDERWRITER')")
    ResponseEntity<ContractResponse> issue(@PathVariable UUID applicationId, Authentication authentication) {
        ContractIssueResult result = contractIssuanceService.issue(applicationId, authentication.getName());
        ContractResponse body = contractMapper.toResponse(result.contract());

        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/contracts/" + body.id())).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
