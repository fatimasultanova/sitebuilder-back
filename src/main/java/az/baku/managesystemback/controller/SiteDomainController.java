package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.RegisterDomainRequest;
import az.baku.managesystemback.dto.response.SiteDomainResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.SiteDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping({"/api/sites/{siteId}/domain", "/sites/{siteId}/domain"})
@RequiredArgsConstructor
public class SiteDomainController {

    private final SiteDomainService siteDomainService;

    @GetMapping
    public ResponseEntity<SiteDomainResponse> getDomain(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long siteId
    ) {
        return ResponseEntity.ok(siteDomainService.getDomain(principal.id(), siteId));
    }

    @PutMapping
    public ResponseEntity<SiteDomainResponse> registerDomain(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long siteId,
            @RequestBody RegisterDomainRequest request
    ) {
        return ResponseEntity.ok(siteDomainService.registerDomain(principal.id(), siteId, request));
    }

    @PostMapping("/verify")
    public ResponseEntity<SiteDomainResponse> verifyDomain(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long siteId
    ) {
        return ResponseEntity.ok(siteDomainService.verifyDomain(principal.id(), siteId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteDomain(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long siteId
    ) {
        siteDomainService.deleteDomain(principal.id(), siteId);
        return ResponseEntity.noContent().build();
    }
}
