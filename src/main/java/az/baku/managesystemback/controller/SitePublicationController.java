package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.CreateDeploymentRequest;
import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/sites/{siteId}/publication")
public class SitePublicationController {
    private final DeploymentService deployments;

    @GetMapping
    public Map<String, Object> status(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long siteId) {
        return deployments.getPublication(user.id(), siteId);
    }
    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public DeploymentResponse publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long siteId) {
        return deployments.createDeployment(user.id(), CreateDeploymentRequest.builder().siteId(siteId).build());
    }
    @DeleteMapping
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void unpublish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long siteId) {
        deployments.unpublish(user.id(), siteId);
    }
}
