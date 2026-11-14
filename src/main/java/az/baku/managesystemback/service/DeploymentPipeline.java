package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the deployment pipeline after the HTTP transaction has committed.
 *
 * @author Fatima Sultanova
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentPipeline {

    private static final String SAFE_FAILURE_MESSAGE = "Deployment failed. Please try again.";
    private static final String INVALID_SITE_MESSAGE = "Deployment failed because the site is not valid.";

    private final DeploymentRepository deploymentRepository;
    private final SiteRepository siteRepository;
    private final AppProperties appProperties;
    private final PlatformTransactionManager transactionManager;

    public void process(long deploymentId) {
        Boolean started;

        try {
            started = newTransaction().execute(status -> markBuilding(deploymentId));
        } catch (RuntimeException ex) {
            log.warn("Could not start deployment {}", deploymentId, ex);
            return;
        }

        if (!Boolean.TRUE.equals(started)) {
            return;
        }

        try {
            newTransaction().executeWithoutResult(status -> buildAndActivate(deploymentId));
        } catch (RuntimeException ex) {
            log.error("Deployment {} failed", deploymentId, ex);
            markFailedIfPending(deploymentId, SAFE_FAILURE_MESSAGE);
        }
    }

    public void markFailedIfPending(long deploymentId, String message) {
        try {
            newTransaction().executeWithoutResult(status -> markFailed(deploymentId, message));
        } catch (RuntimeException ex) {
            log.error("Could not mark deployment {} as failed", deploymentId, ex);
        }
    }

    private Boolean markBuilding(long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId).orElse(null);

        if (deployment == null || deployment.getStatus() != DeploymentStatus.QUEUED) {
            return false;
        }

        deployment.setStatus(DeploymentStatus.BUILDING);
        deployment.setStartedAt(Instant.now());
        deployment.setLogMessage("Deployment started.");
        deploymentRepository.save(deployment);

        return true;
    }

    private void buildAndActivate(long deploymentId) {
        Deployment deployment = deploymentRepository
                .findWithSiteAndTemplateById(deploymentId)
                .orElse(null);

        if (deployment == null || deployment.getStatus() != DeploymentStatus.BUILDING) {
            return;
        }

        Site site = deployment.getSite();
        validateSite(site);

        deployment.setSnapshot(buildSnapshot(site));

        List<Deployment> currentLive = new ArrayList<>(deploymentRepository
                .findBySiteIdAndStatus(site.getId(), DeploymentStatus.LIVE));

        currentLive.removeIf(live -> live.getId() == deployment.getId());

        if (!currentLive.isEmpty()) {
            for (Deployment live : currentLive) {
                live.setStatus(DeploymentStatus.SUPERSEDED);
            }
            deploymentRepository.saveAll(currentLive);
            deploymentRepository.flush();
        }

        deployment.setStatus(DeploymentStatus.LIVE);
        deployment.setPublishedUrl(publishedUrl(site.getSlug()));
        deployment.setFinishedAt(Instant.now());
        deployment.setLogMessage("Deployment is live.");

        site.setStatus(SiteStatus.PUBLISHED);
        siteRepository.save(site);
        deploymentRepository.save(deployment);
    }

    private void markFailed(long deploymentId, String message) {
        Deployment deployment = deploymentRepository.findById(deploymentId).orElse(null);

        if (deployment == null || !deployment.getStatus().isInProgress()) {
            return;
        }

        deployment.setStatus(DeploymentStatus.FAILED);
        deployment.setFinishedAt(Instant.now());
        deployment.setLogMessage(message);
        deploymentRepository.save(deployment);
        log.warn("Deployment {} marked as FAILED: {}", deploymentId, message);
    }

    private void validateSite(Site site) {
        if (site == null
                || site.getSlug() == null || site.getSlug().isBlank()
                || site.getProjectName() == null || site.getProjectName().isBlank()
                || site.getContent() == null) {
            throw new IllegalStateException(INVALID_SITE_MESSAGE);
        }
    }

    private Map<String, Object> buildSnapshot(Site site) {
        Map<String, Object> siteSnapshot = new LinkedHashMap<>();
        siteSnapshot.put("id", site.getId());
        siteSnapshot.put("projectName", site.getProjectName());
        siteSnapshot.put("slug", site.getSlug());
        siteSnapshot.put("status", site.getStatus() == null ? null : site.getStatus().name());
        siteSnapshot.put("content", site.getContent());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("site", siteSnapshot);
        snapshot.put("template", templateSnapshot(site.getTemplate()));
        snapshot.put("capturedAt", Instant.now().toString());

        return snapshot;
    }

    private Map<String, Object> templateSnapshot(Template template) {
        if (template == null) {
            return null;
        }

        Map<String, Object> templateSnapshot = new LinkedHashMap<>();
        templateSnapshot.put("id", template.getId());
        templateSnapshot.put("slug", template.getSlug());
        templateSnapshot.put("name", template.getName());
        templateSnapshot.put("category", template.getCategory());
        templateSnapshot.put("themeConfig", template.getThemeConfig());
        templateSnapshot.put("translations", template.getTranslations());
        templateSnapshot.put("structure", template.getStructure());

        return templateSnapshot;
    }

    private String publishedUrl(String slug) {
        String template = appProperties.getPublishedUrlTemplate();

        if (template == null || template.isBlank() || slug == null) {
            return null;
        }

        return template.trim().replace("{slug}", slug);
    }

    private TransactionTemplate newTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
