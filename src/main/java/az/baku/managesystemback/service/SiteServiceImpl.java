package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.dto.request.UpdateSiteRequest;
import az.baku.managesystemback.dto.response.SiteResponse;
import az.baku.managesystemback.dto.response.SiteSummaryResponse;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.mapper.SiteMapper;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.TemplateRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteServiceImpl implements SiteService {

    private static final String SITE_NOT_FOUND_MESSAGE = "Site not found.";
    private static final String TEMPLATE_NOT_FOUND_MESSAGE = "Template not found.";
    private static final String TEMPLATE_UNAVAILABLE_MESSAGE = "The selected template is not available.";
    private static final String SLUG_IN_USE_MESSAGE = "This URL slug is already in use.";

    private final SiteRepository siteRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SiteResponse createSite(long userId, CreateSiteRequest request) {
        Workspace workspace = getOrCreateDefaultWorkspace(userId);
        Template template = getAvailableTemplate(request.getTemplateId());
        String slug = SiteMapper.normalizeSlug(request.getSlug());

        if (siteRepository.existsByWorkspaceIdAndSlug(workspace.getId(), slug)) {
            throw slugConflict();
        }

        Site site = SiteMapper.toEntity(request, workspace, template);
        Site savedSite = siteRepository.save(site);

        return SiteMapper.toResponse(savedSite);
    }

    @Override
    public List<SiteSummaryResponse> listSites(long userId) {
        return siteRepository.findAllByWorkspaceOwnerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SiteMapper::toSummary)
                .toList();
    }

    @Override
    public SiteResponse getSite(long userId, long siteId) {
        Site site = findOwnedSite(userId, siteId);
        return SiteMapper.toResponse(site);
    }

    @Override
    @Transactional
    public SiteResponse updateSite(long userId, long siteId, UpdateSiteRequest request) {
        Site site = findOwnedSite(userId, siteId);
        Template template = request.getTemplateId() == null
                ? null
                : getAvailableTemplate(request.getTemplateId());
        String slug = SiteMapper.normalizeSlug(request.getSlug());

        if (siteRepository.existsByWorkspaceIdAndSlugAndIdNot(site.getWorkspace().getId(), slug, siteId)) {
            throw slugConflict();
        }

        SiteMapper.applyUpdate(site, request, template);
        Site savedSite = siteRepository.save(site);

        return SiteMapper.toResponse(savedSite);
    }

    @Override
    @Transactional
    public void deleteSite(long userId, long siteId) {
        Site site = findOwnedSite(userId, siteId);
        siteRepository.delete(site);
    }

    private Site findOwnedSite(long userId, long siteId) {
        return siteRepository.findByIdAndWorkspaceOwnerId(siteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, SITE_NOT_FOUND_MESSAGE));
    }

    private Template getAvailableTemplate(long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, TEMPLATE_NOT_FOUND_MESSAGE));

        if (!template.isPublished()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, TEMPLATE_UNAVAILABLE_MESSAGE);
        }

        return template;
    }

    private Workspace getOrCreateDefaultWorkspace(long userId) {
        return workspaceRepository.findFirstByOwnerIdOrderByIdAsc(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Authenticated user not found."
                            ));

                    Workspace workspace = Workspace.builder()
                            .name("Personal Workspace")
                            .owner(user)
                            .build();

                    return workspaceRepository.save(workspace);
                });
    }

    private ApiException slugConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                SLUG_IN_USE_MESSAGE,
                Map.of("slug", List.of(SLUG_IN_USE_MESSAGE))
        );
    }
}
