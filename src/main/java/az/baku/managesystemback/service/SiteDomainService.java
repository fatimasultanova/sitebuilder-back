package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.RegisterDomainRequest;
import az.baku.managesystemback.dto.response.SiteDomainResponse;

/**
 * @author Fatima Sultanova
 */
public interface SiteDomainService {

    SiteDomainResponse getDomain(long userId, long siteId);

    SiteDomainResponse registerDomain(long userId, long siteId, RegisterDomainRequest request);

    SiteDomainResponse verifyDomain(long userId, long siteId);

    void deleteDomain(long userId, long siteId);

    String resolveConnectedSiteSlug(String host);
}
