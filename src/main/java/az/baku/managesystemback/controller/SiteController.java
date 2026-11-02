package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.dto.request.UpdateSiteRequest;
import az.baku.managesystemback.dto.response.SiteResponse;
import az.baku.managesystemback.dto.response.SiteSummaryResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.SiteService;
import jakarta.validation.Valid;
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

import java.net.URI;
import java.util.List;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    public ResponseEntity<SiteResponse> createSite(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateSiteRequest request
    ) {
        SiteResponse response = siteService.createSite(principal.id(), request);
        return ResponseEntity
                .created(URI.create("/api/sites/" + response.getId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SiteSummaryResponse>> listSites(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(siteService.listSites(principal.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getSite(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(siteService.getSite(principal.id(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteResponse> updateSite(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id,
            @Valid @RequestBody UpdateSiteRequest request
    ) {
        return ResponseEntity.ok(siteService.updateSite(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        siteService.deleteSite(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
