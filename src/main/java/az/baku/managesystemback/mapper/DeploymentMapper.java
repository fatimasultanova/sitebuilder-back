package az.baku.managesystemback.mapper;

import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.dto.response.PublicSiteResponse;
import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.entity.Site;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author Fatima Sultanova
 */
public final class DeploymentMapper {

    private DeploymentMapper() {
    }

    public static DeploymentResponse toResponse(Deployment deployment) {
        return DeploymentResponse.builder()
                .id(String.valueOf(deployment.getId()))
                .siteId(deployment.getSite() == null
                        ? null
                        : String.valueOf(deployment.getSite().getId()))
                .version(deployment.getVersion())
                .status(toStatusValue(deployment.getStatus()))
                .publishedUrl(deployment.getPublishedUrl())
                .logMessage(deployment.getLogMessage())
                .triggeredBy(deployment.getTriggeredBy())
                .createdAt(toTimestamp(deployment.getCreatedAt()))
                .startedAt(toTimestamp(deployment.getStartedAt()))
                .finishedAt(toTimestamp(deployment.getFinishedAt()))
                .build();
    }

    public static PublicSiteResponse toPublicResponse(Deployment deployment) {
        Site site = deployment.getSite();

        return PublicSiteResponse.builder()
                .siteId(site == null ? null : String.valueOf(site.getId()))
                .slug(site == null ? null : site.getSlug())
                .projectName(site == null ? null : site.getProjectName())
                .version(deployment.getVersion())
                .publishedUrl(deployment.getPublishedUrl())
                .publishedAt(toTimestamp(deployment.getFinishedAt() == null
                        ? deployment.getCreatedAt()
                        : deployment.getFinishedAt()))
                .snapshot(deployment.getSnapshot())
                .build();
    }

    public static String toStatusValue(DeploymentStatus status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }

    public static String toTimestamp(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
