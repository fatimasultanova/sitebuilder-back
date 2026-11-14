package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.response.PublicSiteResponse;
import az.baku.managesystemback.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping("/api/public/sites")
@RequiredArgsConstructor
public class PublicSiteController {

    private final DeploymentService deploymentService;

    @GetMapping("/{slug}")
    public ResponseEntity<PublicSiteResponse> getPublishedSite(@PathVariable String slug) {
        return ResponseEntity.ok(deploymentService.getPublicLiveSite(slug));
    }
}
