package az.baku.managesystemback.mapper;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.dto.request.UpdateSiteRequest;
import az.baku.managesystemback.dto.response.SiteResponse;
import az.baku.managesystemback.dto.response.SiteSummaryResponse;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.Workspace;

import java.util.Locale;

/**
 * @author Fatima Sultanova
 */
public final class SiteMapper {

    private SiteMapper() {
    }

    public static Site toEntity(
            CreateSiteRequest request,
            Workspace workspace,
            Template template
    ) {
        return Site.builder()
                .workspace(workspace)
                .template(template)
                .title(request.getProjectName().trim())
                .slug(normalizeSlug(request.getSlug()))
                .contentJson(request.getContent())
                .status(request.getStatus() == null ? SiteStatus.DRAFT : request.getStatus())
                .build();
    }

    public static void applyUpdate(Site site, UpdateSiteRequest request, Template template) {
        site.setTitle(request.getProjectName().trim());
        site.setSlug(normalizeSlug(request.getSlug()));
        site.setContentJson(request.getContent());

        if (template != null) {
            site.setTemplate(template);
        }
        if (request.getStatus() != null) {
            site.setStatus(request.getStatus());
        }
    }

    public static SiteResponse toResponse(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .templateId(templateId(site))
                .templateName(templateName(site))
                .projectName(site.getTitle())
                .slug(site.getSlug())
                .status(site.getStatus())
                .content(site.getContentJson())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }

    public static SiteSummaryResponse toSummary(Site site) {
        return SiteSummaryResponse.builder()
                .id(site.getId())
                .templateId(templateId(site))
                .templateName(templateName(site))
                .projectName(site.getTitle())
                .slug(site.getSlug())
                .status(site.getStatus())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }

    public static String normalizeSlug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase(Locale.ROOT);
    }

    private static Long templateId(Site site) {
        return site.getTemplate() == null ? null : site.getTemplate().getId();
    }

    private static String templateName(Site site) {
        return site.getTemplate() == null ? null : site.getTemplate().getName();
    }
}
