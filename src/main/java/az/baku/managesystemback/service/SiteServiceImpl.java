package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.dto.request.UpdateSiteRequest;
import az.baku.managesystemback.dto.response.SiteResponse;
import az.baku.managesystemback.dto.response.SiteSummaryResponse;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.mapper.SiteMapper;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.TemplateRepository;
import az.baku.managesystemback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteServiceImpl implements SiteService {

    private static final int MAX_SLUG_LENGTH = 80;
    private static final String SITE_NOT_FOUND_MESSAGE = "Site not found.";
    private static final String TEMPLATE_UNAVAILABLE_MESSAGE = "The selected template is not available.";
    private static final String SLUG_IN_USE_MESSAGE = "This URL slug is already in use.";

    private final SiteRepository siteRepository;
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SiteResponse createSite(long userId, CreateSiteRequest request) {
        User owner = getOwner(userId);
        Template template = getActiveTemplate(request.getTemplateId());
        String slug = SiteMapper.normalizeSlug(request.getSlug());

        if (siteRepository.existsBySlug(slug)) {
            throw slugConflict();
        }

        Site site = SiteMapper.toEntity(request, owner, template);
        Site savedSite = siteRepository.save(site);

        return SiteMapper.toResponse(savedSite);
    }

    @Override
    public List<SiteSummaryResponse> listSites(long userId, SiteStatus status, String search) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        return siteRepository.findAllForOwner(userId, status, normalizedSearch)
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
                : getActiveTemplate(request.getTemplateId());
        String slug = SiteMapper.normalizeSlug(request.getSlug());

        if (siteRepository.existsBySlugAndIdNot(slug, siteId)) {
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

    @Override
    @Transactional
    public SiteResponse duplicateSite(long userId, long siteId) {
        Site source = findOwnedSite(userId, siteId);

        Site copy = Site.builder()
                .owner(source.getOwner())
                .template(source.getTemplate())
                .projectName(source.getProjectName())
                .slug(generateCopySlug(source.getSlug()))
                .status(SiteStatus.DRAFT)
                .content(deepCopyContent(source.getContent()))
                .build();

        Site savedSite = siteRepository.save(copy);

        return SiteMapper.toResponse(savedSite);
    }

    private Site findOwnedSite(long userId, long siteId) {
        return siteRepository.findByIdAndOwnerId(siteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, SITE_NOT_FOUND_MESSAGE));
    }

    private Template getActiveTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .filter(Template::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        TEMPLATE_UNAVAILABLE_MESSAGE,
                        Map.of("templateId", List.of(TEMPLATE_UNAVAILABLE_MESSAGE))
                ));
    }

    private User getOwner(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));
    }

    private String generateCopySlug(String baseSlug) {
        String base = baseSlug == null ? "site" : baseSlug;
        int suffix = 1;

        while (true) {
            String suffixPart = suffix == 1 ? "-copy" : "-copy-" + suffix;
            int maxBaseLength = MAX_SLUG_LENGTH - suffixPart.length();
            String truncatedBase = base.length() > maxBaseLength
                    ? base.substring(0, Math.max(1, maxBaseLength))
                    : base;
            truncatedBase = truncatedBase.replaceAll("-+$", "");

            if (truncatedBase.isEmpty()) {
                truncatedBase = "site";
            }

            String candidate = truncatedBase + suffixPart;

            if (!siteRepository.existsBySlug(candidate)) {
                return candidate;
            }

            suffix++;
        }
    }

    private static Map<String, Object> deepCopyContent(Map<String, Object> content) {
        if (content == null) {
            return new LinkedHashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) deepCopyValue(content);
        return copy;
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
            }
            return copy;
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(deepCopyValue(item));
            }
            return copy;
        }

        return value;
    }

    private ApiException slugConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                SLUG_IN_USE_MESSAGE,
                Map.of("slug", List.of(SLUG_IN_USE_MESSAGE))
        );
    }
}
