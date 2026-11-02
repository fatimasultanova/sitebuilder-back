package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.dto.request.UpdateSiteRequest;
import az.baku.managesystemback.dto.response.SiteResponse;
import az.baku.managesystemback.dto.response.SiteSummaryResponse;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
public interface SiteService {

    SiteResponse createSite(long userId, CreateSiteRequest request);

    List<SiteSummaryResponse> listSites(long userId);

    SiteResponse getSite(long userId, long siteId);

    SiteResponse updateSite(long userId, long siteId, UpdateSiteRequest request);

    void deleteSite(long userId, long siteId);
}
