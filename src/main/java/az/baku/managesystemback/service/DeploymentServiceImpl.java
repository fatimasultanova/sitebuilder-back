package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.dto.request.CreateDeploymentRequest;
import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.dto.response.PublicSiteResponse;
import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.event.DeploymentRequestedEvent;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.mapper.DeploymentMapper;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeploymentServiceImpl implements DeploymentService {

    private static final String DEPLOYMENT_NOT_FOUND_MESSAGE = "Deployment not found.";
    private static final String SITE_NOT_FOUND_MESSAGE = "Site not found.";
    private static final String IN_PROGRESS_MESSAGE = "A deployment is already in progress for this site.";
    private static final String VERSION_EXISTS_MESSAGE = "This deployment version already exists for the site.";
    private static final String CANNOT_STOP_MESSAGE = "Only queued or building deployments can be stopped.";
    private static final String CANNOT_REDEPLOY_MESSAGE = "Only finished deployments can be redeployed.";
    private static final String CANNOT_ROLLBACK_MESSAGE = "Only superseded deployments can be rolled back.";
    private static final String INVALID_STATUS_MESSAGE = "Invalid deployment status.";
    private static final String PUBLIC_SITE_NOT_FOUND_MESSAGE = "Published site not found.";

    private final DeploymentRepository deploymentRepository;
    private final SiteRepository siteRepository;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ActivityService activity;

    @Override
    public List<DeploymentResponse> listDeployments(long userId, Long siteId, String status) {
        if (siteId != null) {
            findOwnedSite(userId, siteId);
        }

        DeploymentStatus statusFilter = parseStatus(status);

        return deploymentRepository.findAllForOwner(userId, siteId, statusFilter)
                .stream()
                .map(DeploymentMapper::toResponse)
                .toList();
    }

    @Override
    public DeploymentResponse getDeployment(long userId, long deploymentId) {
        return DeploymentMapper.toResponse(findOwnedDeployment(userId, deploymentId));
    }

    @Override
    @Transactional
    public DeploymentResponse createDeployment(long userId, CreateDeploymentRequest request) {
        if (request == null || request.getSiteId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Site id is required.",
                    Map.of("siteId", List.of("Site id is required."))
            );
        }

        Site site = findOwnedSite(userId, request.getSiteId());
        return enqueue(site, request.getVersion());
    }

    @Override
    @Transactional
    public DeploymentResponse redeploy(long userId, long deploymentId) {
        Deployment source = findOwnedDeployment(userId, deploymentId);

        if (!source.getStatus().isFinished()) {
            throw new ApiException(HttpStatus.CONFLICT, CANNOT_REDEPLOY_MESSAGE);
        }

        return enqueue(source.getSite(), null);
    }

    @Override
    @Transactional
    public DeploymentResponse stop(long userId, long deploymentId) {
        Deployment deployment = findOwnedDeployment(userId, deploymentId);

        if (!deployment.getStatus().isInProgress()) {
            throw new ApiException(HttpStatus.CONFLICT, CANNOT_STOP_MESSAGE);
        }

        deployment.setStatus(DeploymentStatus.STOPPED);
        deployment.setFinishedAt(Instant.now());
        deployment.setLogMessage("Deployment stopped by user.");
        activity.record(deployment.getSite(), "deployment.changed", "Deployment stopped.");

        return DeploymentMapper.toResponse(deploymentRepository.save(deployment));
    }

    @Override
    @Transactional
    public DeploymentResponse rollback(long userId, long deploymentId) {
        Deployment target = findOwnedDeployment(userId, deploymentId);

        if (target.getStatus() != DeploymentStatus.SUPERSEDED) {
            throw new ApiException(HttpStatus.CONFLICT, CANNOT_ROLLBACK_MESSAGE);
        }

        Site site = target.getSite();
        siteRepository.lockForPublication(site.getId());
        if (deploymentRepository.existsBySiteIdAndStatusIn(site.getId(),
                List.of(DeploymentStatus.QUEUED, DeploymentStatus.BUILDING))) {
            throw new ApiException(HttpStatus.CONFLICT, IN_PROGRESS_MESSAGE);
        }
        List<Deployment> currentLive = new ArrayList<>(deploymentRepository
                .findBySiteIdAndStatus(site.getId(), DeploymentStatus.LIVE));

        currentLive.removeIf(deployment -> deployment.getId() == target.getId());
        for (Deployment deployment : currentLive) {
            deployment.setStatus(DeploymentStatus.SUPERSEDED);
        }

        if (!currentLive.isEmpty()) {
            deploymentRepository.saveAll(currentLive);
            deploymentRepository.flush();
        }

        target.setStatus(DeploymentStatus.LIVE);
        target.setPublishedUrl(publishedUrl(site.getSlug()));
        target.setFinishedAt(Instant.now());
        target.setLogMessage("Rolled back to version " + target.getVersion() + ".");

        site.setStatus(SiteStatus.PUBLISHED);
        siteRepository.save(site);
        activity.record(site, "deployment.changed", "Rolled back to version " + target.getVersion() + ".");

        return DeploymentMapper.toResponse(deploymentRepository.save(target));
    }

    @Override
    public PublicSiteResponse getPublicLiveSite(String slug) {
        String normalizedSlug = slug == null ? null : slug.trim().toLowerCase(Locale.ROOT);

        return deploymentRepository
                .findLiveBySiteSlug(normalizedSlug, DeploymentStatus.LIVE)
                .stream()
                .findFirst()
                .map(DeploymentMapper::toPublicResponse)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        PUBLIC_SITE_NOT_FOUND_MESSAGE
                ));
    }

    private DeploymentResponse enqueue(Site site, Integer requestedVersion) {
        siteRepository.lockForPublication(site.getId());
        if (deploymentRepository.existsBySiteIdAndStatusIn(
                site.getId(),
                List.of(DeploymentStatus.QUEUED, DeploymentStatus.BUILDING)
        )) {
            throw new ApiException(HttpStatus.CONFLICT, IN_PROGRESS_MESSAGE);
        }

        Integer version = resolveVersion(site.getId(), requestedVersion);

        Deployment deployment = Deployment.builder()
                .site(site)
                .owner(site.getOwner())
                .version(version)
                .status(DeploymentStatus.QUEUED)
                .triggeredBy(triggeredBy(site))
                .logMessage("Deployment queued.")
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        activity.record(site, "deployment.changed", "Deployment queued.");
        eventPublisher.publishEvent(new DeploymentRequestedEvent(saved.getId()));

        return DeploymentMapper.toResponse(saved);
    }

    private Integer resolveVersion(long siteId, Integer requestedVersion) {
        if (requestedVersion == null) {
            int nextVersion = deploymentRepository.findMaxVersionBySiteId(siteId) + 1;

            while (deploymentRepository.existsBySiteIdAndVersion(siteId, nextVersion)) {
                nextVersion++;
            }

            return nextVersion;
        }

        if (requestedVersion <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Version must be a positive number.",
                    Map.of("version", List.of("Version must be a positive number."))
            );
        }

        if (deploymentRepository.existsBySiteIdAndVersion(siteId, requestedVersion)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    VERSION_EXISTS_MESSAGE,
                    Map.of("version", List.of(VERSION_EXISTS_MESSAGE))
            );
        }

        return requestedVersion;
    }

    @Override
    public Map<String, Object> getPublication(long userId, long siteId) {
        findOwnedSite(userId, siteId);
        List<Deployment> live = deploymentRepository.findBySiteIdAndStatus(siteId, DeploymentStatus.LIVE);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("available", true);
        result.put("status", live.isEmpty() ? "unpublished" : "published");
        result.put("publishedUrl", live.isEmpty() ? null : live.get(0).getPublishedUrl());
        result.put("pending", deploymentRepository.existsBySiteIdAndStatusIn(siteId,
                List.of(DeploymentStatus.QUEUED, DeploymentStatus.BUILDING)));
        return result;
    }

    @Override
    @Transactional
    public void unpublish(long userId, long siteId) {
        Site site = findOwnedSite(userId, siteId);
        siteRepository.lockForPublication(siteId);
        var active = deploymentRepository.findBySiteIdAndStatusIn(siteId,
                List.of(DeploymentStatus.LIVE, DeploymentStatus.QUEUED, DeploymentStatus.BUILDING));
        for (Deployment deployment : active) {
            deployment.setStatus(deployment.getStatus() == DeploymentStatus.LIVE
                    ? DeploymentStatus.SUPERSEDED : DeploymentStatus.STOPPED);
            deployment.setFinishedAt(Instant.now());
            deployment.setLogMessage("Site unpublished by user.");
        }
        deploymentRepository.saveAll(active);
        site.setStatus(SiteStatus.DRAFT);
        siteRepository.save(site);
        if (!active.isEmpty()) activity.record(site, "deployment.changed", "Site unpublished.");
    }

    private DeploymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return DeploymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    INVALID_STATUS_MESSAGE,
                    Map.of("status", List.of(INVALID_STATUS_MESSAGE))
            );
        }
    }

    private Deployment findOwnedDeployment(long userId, long deploymentId) {
        return deploymentRepository.findByIdAndOwnerId(deploymentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, DEPLOYMENT_NOT_FOUND_MESSAGE));
    }

    private Site findOwnedSite(long userId, long siteId) {
        return siteRepository.findByIdAndOwnerId(siteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, SITE_NOT_FOUND_MESSAGE));
    }

    private String triggeredBy(Site site) {
        if (site.getOwner() == null || site.getOwner().getEmail() == null) {
            return null;
        }

        return site.getOwner().getEmail();
    }

    private String publishedUrl(String slug) {
        String template = appProperties.getPublishedUrlTemplate();

        if (template == null || template.isBlank() || slug == null) {
            return null;
        }

        return template.trim().replace("{slug}", slug);
    }
}
