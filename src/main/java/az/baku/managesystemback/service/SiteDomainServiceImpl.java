package az.baku.managesystemback.service;

import az.baku.managesystemback.config.DomainProperties;
import az.baku.managesystemback.dto.request.RegisterDomainRequest;
import az.baku.managesystemback.dto.response.DnsRecordResponse;
import az.baku.managesystemback.dto.response.SiteDomainResponse;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteDomain;
import az.baku.managesystemback.entity.SiteDomainSslStatus;
import az.baku.managesystemback.entity.SiteDomainStatus;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.mapper.SiteDomainMapper;
import az.baku.managesystemback.repository.SiteDomainRepository;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Fatima Sultanova
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteDomainServiceImpl implements SiteDomainService {

    private static final int MAX_HOSTNAME_LENGTH = 253;
    private static final int MAX_LABEL_LENGTH = 63;
    private static final String TXT_RECORD_PREFIX = "_sitebuilder-verify.";
    private static final String VERIFICATION_VALUE_PREFIX = "sitebuilder-verification=";

    private static final String SITE_NOT_FOUND_MESSAGE = "Site not found.";
    private static final String DOMAIN_NOT_CONFIGURED_MESSAGE = "No custom domain is configured for this site.";
    private static final String HOSTNAME_IN_USE_MESSAGE = "This domain is already in use.";
    private static final String INVALID_HOSTNAME_MESSAGE =
            "Enter a valid domain name (for example, example.com).";
    private static final String PLATFORM_DOMAIN_MESSAGE =
            "This domain belongs to the platform and cannot be used.";
    private static final String VERIFICATION_PENDING_MESSAGE =
            "The verification TXT record was not found yet.";
    private static final String DNS_LOOKUP_FAILED_MESSAGE =
            "DNS lookup failed. Please try again later.";
    private static final String PUBLIC_SITE_NOT_FOUND_MESSAGE = "Published site not found.";

    private static final Pattern LABEL_PATTERN =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){3}$");

    private final SiteDomainRepository siteDomainRepository;
    private final SiteRepository siteRepository;
    private final PlanLimitService planLimitService;
    private final DnsResolver dnsResolver;
    private final SslProvisioner sslProvisioner;
    private final DomainProperties domainProperties;
    private final ActivityService activity;

    @Override
    public SiteDomainResponse getDomain(long userId, long siteId) {
        findOwnedSite(userId, siteId);

        return siteDomainRepository.findBySiteId(siteId)
                .map(this::toResponse)
                .orElseGet(SiteDomainMapper::unconfigured);
    }

    @Override
    @Transactional
    public SiteDomainResponse registerDomain(long userId, long siteId, RegisterDomainRequest request) {
        Site site = findOwnedSite(userId, siteId);
        String hostname = validateHostname(request == null ? null : request.getHostname());

        Optional<SiteDomain> domainWithHostname = siteDomainRepository.findByHostname(hostname);
        if (domainWithHostname.isPresent()
                && domainWithHostname.get().getSite().getId() != siteId) {
            throw hostnameConflict();
        }

        planLimitService.requireCustomDomainAllowed(userId);

        SiteDomain domain = domainWithHostname
                .filter(existing -> existing.getSite().getId() == siteId)
                .or(() -> siteDomainRepository.findBySiteId(siteId))
                .orElseGet(() -> SiteDomain.builder().site(site).build());

        if (hostname.equals(domain.getHostname())) {
            return toResponse(domain); // Retrying PUT must not invalidate an existing TXT token.
        }

        domain.setHostname(hostname);
        domain.setVerificationToken(TokenUtils.generateToken());
        domain.setStatus(SiteDomainStatus.PENDING);
        domain.setSslStatus(SiteDomainSslStatus.PENDING);
        domain.setVerifiedAt(null);
        domain.setLastCheckedAt(null);
        domain.setErrorMessage(null);
        activity.record(site, "domain.changed", "Domain attached.");

        return toResponse(siteDomainRepository.save(domain));
    }

    @Override
    @Transactional
    public SiteDomainResponse verifyDomain(long userId, long siteId) {
        findOwnedSite(userId, siteId);

        SiteDomain domain = siteDomainRepository.findBySiteId(siteId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        DOMAIN_NOT_CONFIGURED_MESSAGE
                ));

        LocalDateTime checkedAt = LocalDateTime.now();
        SiteDomainStatus previousStatus = domain.getStatus();
        domain.setLastCheckedAt(checkedAt);

        String expectedValue = VERIFICATION_VALUE_PREFIX + domain.getVerificationToken();

        try {
            boolean verified = dnsResolver
                    .lookupTxt(TXT_RECORD_PREFIX + domain.getHostname())
                    .stream()
                    .map(SiteDomainServiceImpl::normalizeTxtValue)
                    .anyMatch(expectedValue::equals);

            if (verified) {
                domain.setStatus(SiteDomainStatus.CONNECTED);
                domain.setVerifiedAt(checkedAt);
                domain.setErrorMessage(null);
                provisionSsl(domain);
            } else {
                domain.setStatus(SiteDomainStatus.PENDING);
                domain.setVerifiedAt(null);
                domain.setSslStatus(SiteDomainSslStatus.PENDING);
                domain.setErrorMessage(VERIFICATION_PENDING_MESSAGE);
            }
        } catch (DnsLookupException ex) {
            // A temporary resolver outage does not revoke previously proven ownership.
            domain.setErrorMessage(DNS_LOOKUP_FAILED_MESSAGE);
            log.warn("DNS lookup failed while verifying hostname {}: {}",
                    domain.getHostname(), ex.getMessage());
        }

        if (domain.getStatus() != previousStatus) {
            activity.record(domain.getSite(), "domain.changed",
                    "Domain verification: " + domain.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }
        return toResponse(siteDomainRepository.save(domain));
    }

    @Override
    @Transactional
    public void deleteDomain(long userId, long siteId) {
        findOwnedSite(userId, siteId);
        siteDomainRepository.findBySiteId(siteId)
                .ifPresent(domain -> {
                    activity.record(domain.getSite(), "domain.changed", "Domain removed.");
                    siteDomainRepository.delete(domain);
                });
    }

    @Override
    public String resolveConnectedSiteSlug(String host) {
        String normalizedHost = host == null ? null : host.trim().toLowerCase(Locale.ROOT);

        if (normalizedHost == null || normalizedHost.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, PUBLIC_SITE_NOT_FOUND_MESSAGE);
        }

        return siteDomainRepository.findByHostname(normalizedHost)
                .filter(domain -> domain.getStatus() == SiteDomainStatus.CONNECTED)
                .map(domain -> domain.getSite().getSlug())
                .filter(slug -> slug != null && !slug.isBlank())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        PUBLIC_SITE_NOT_FOUND_MESSAGE
                ));
    }

    private Site findOwnedSite(long userId, long siteId) {
        return siteRepository.findByIdAndOwnerId(siteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, SITE_NOT_FOUND_MESSAGE));
    }

    private String validateHostname(String rawHostname) {
        if (rawHostname == null) {
            throw invalidHostname();
        }

        String hostname = rawHostname.trim().toLowerCase(Locale.ROOT);

        if (hostname.isEmpty()
                || hostname.length() > MAX_HOSTNAME_LENGTH
                || hostname.contains("://")
                || hostname.contains("/")
                || hostname.contains(":")
                || hostname.contains("?")
                || hostname.contains("#")
                || hostname.contains("@")
                || hostname.contains("*")
                || hostname.chars().anyMatch(Character::isWhitespace)) {
            throw invalidHostname();
        }

        if (hostname.equals("localhost") || hostname.endsWith(".localhost")) {
            throw invalidHostname();
        }

        if (IPV4_PATTERN.matcher(hostname).matches()) {
            throw invalidHostname();
        }

        String[] labels = hostname.split("\\.", -1);
        if (labels.length < 2) {
            throw invalidHostname();
        }

        for (String label : labels) {
            if (label.isEmpty()
                    || label.length() > MAX_LABEL_LENGTH
                    || !LABEL_PATTERN.matcher(label).matches()) {
                throw invalidHostname();
            }
        }

        if (!com.google.common.net.InternetDomainName.isValid(hostname)
                || com.google.common.net.InternetDomainName.from(hostname).isPublicSuffix()) {
            throw invalidHostname();
        }
        enforcePlatformDomain(hostname);

        return hostname;
    }

    private void enforcePlatformDomain(String hostname) {
        String platformBaseDomain = domainProperties.getPlatformBaseDomain();

        if (platformBaseDomain == null || platformBaseDomain.isBlank()) {
            return;
        }

        String normalizedBase = platformBaseDomain.trim().toLowerCase(Locale.ROOT);

        if (hostname.equals(normalizedBase) || hostname.endsWith("." + normalizedBase)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    PLATFORM_DOMAIN_MESSAGE,
                    Map.of("hostname", List.of(PLATFORM_DOMAIN_MESSAGE))
            );
        }
    }

    private SiteDomainResponse toResponse(SiteDomain domain) {
        return SiteDomainMapper.toResponse(domain, buildDnsRecords(domain));
    }

    private List<DnsRecordResponse> buildDnsRecords(SiteDomain domain) {
        List<DnsRecordResponse> records = new ArrayList<>();

        records.add(DnsRecordResponse.builder()
                .type("TXT")
                .name(TXT_RECORD_PREFIX + domain.getHostname())
                .value(VERIFICATION_VALUE_PREFIX + domain.getVerificationToken())
                .build());

        if (isApex(domain.getHostname())) {
            List<String> aRecords = domainProperties.getARecords();

            if (aRecords != null) {
                for (String aRecord : aRecords) {
                    if (aRecord == null || aRecord.isBlank()) {
                        continue;
                    }

                    records.add(DnsRecordResponse.builder()
                            .type("A")
                            .name(domain.getHostname())
                            .value(aRecord.trim())
                            .build());
                }
            }
        } else {
            String cnameTarget = domainProperties.getCnameTarget();

            if (cnameTarget != null && !cnameTarget.isBlank()) {
                records.add(DnsRecordResponse.builder()
                        .type("CNAME")
                        .name(domain.getHostname())
                        .value(cnameTarget.trim())
                        .build());
            }
        }

        return List.copyOf(records);
    }

    private boolean isApex(String hostname) {
        var name = com.google.common.net.InternetDomainName.from(hostname);
        return name.hasPublicSuffix() ? name.isTopPrivateDomain()
                : hostname.chars().filter(character -> character == '.').count() == 1;
    }

    private void provisionSsl(SiteDomain domain) {
        try {
            sslProvisioner.provision(domain.getHostname());
        } catch (RuntimeException ex) {
            log.warn("SSL provisioning could not be started for hostname {}: {}",
                    domain.getHostname(), ex.getMessage());
        }
    }

    private static String normalizeTxtValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replace("\"", "");
    }

    private ApiException invalidHostname() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                INVALID_HOSTNAME_MESSAGE,
                Map.of("hostname", List.of(INVALID_HOSTNAME_MESSAGE))
        );
    }

    private ApiException hostnameConflict() {
        return new ApiException(
                HttpStatus.CONFLICT,
                HOSTNAME_IN_USE_MESSAGE,
                Map.of("hostname", List.of(HOSTNAME_IN_USE_MESSAGE))
        );
    }
}
