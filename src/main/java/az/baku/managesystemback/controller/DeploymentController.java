package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.CreateDeploymentRequest;
import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping({"/api/deployments", "/deployments"})
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> listDeployments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(deploymentService.listDeployments(principal.id(), siteId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeploymentResponse> getDeployment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(deploymentService.getDeployment(principal.id(), id));
    }

    @PostMapping
    public ResponseEntity<DeploymentResponse> createDeployment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody CreateDeploymentRequest request
    ) {
        DeploymentResponse response = deploymentService.createDeployment(principal.id(), request);
        return ResponseEntity
                .created(URI.create("/api/deployments/" + response.getId()))
                .body(response);
    }

    @PostMapping("/{id}/redeploy")
    public ResponseEntity<DeploymentResponse> redeploy(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        DeploymentResponse response = deploymentService.redeploy(principal.id(), id);
        return ResponseEntity
                .created(URI.create("/api/deployments/" + response.getId()))
                .body(response);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<DeploymentResponse> stop(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(deploymentService.stop(principal.id(), id));
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<DeploymentResponse> rollback(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(deploymentService.rollback(principal.id(), id));
    }
}
