package az.baku.managesystemback.service;

import az.baku.managesystemback.config.DomainProperties;
import az.baku.managesystemback.dto.request.RegisterDomainRequest;
import az.baku.managesystemback.dto.response.DnsRecordResponse;
import az.baku.managesystemback.dto.response.SiteDomainResponse;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteDomain;
import az.baku.managesystemback.entity.SiteDomainSslStatus;
import az.baku.managesystemback.entity.SiteDomainStatus;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.SiteDomainRepository;
import az.baku.managesystemback.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SiteDomainServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long SITE_ID = 100L;

    private SiteDomainRepository siteDomainRepository;
    private SiteRepository siteRepository;
    private PlanLimitService planLimitService;
    private DnsResolver dnsResolver;
    private SslProvisioner sslProvisioner;
    private DomainProperties domainProperties;
    private SiteDomainServiceImpl service;
    private Site site;

    @BeforeEach
    void setUp() {
        siteDomainRepository = mock(SiteDomainRepository.class);
        siteRepository = mock(SiteRepository.class);
        planLimitService = mock(PlanLimitService.class);
        dnsResolver = mock(DnsResolver.class);
        sslProvisioner = mock(SslProvisioner.class);

        domainProperties = new DomainProperties();
        domainProperties.setCnameTarget("sites.example.net");
        domainProperties.setARecords(List.of("203.0.113.10", "203.0.113.11"));
        domainProperties.setPlatformBaseDomain("sitebuilder.example");

        service = new SiteDomainServiceImpl(
                siteDomainRepository,
                siteRepository,
                planLimitService,
                dnsResolver,
                sslProvisioner,
                domainProperties
        );

        User owner = User.builder()
                .id(USER_ID)
                .name("Test")
                .email("test@example.com")
                .password("secret")
                .build();

        site = Site.builder()
                .id(SITE_ID)
                .owner(owner)
                .projectName("My Site")
                .slug("my-site")
                .build();

        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(siteDomainRepository.save(any(SiteDomain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerNormalizesHostnameAndReturnsApexRecords() {
        SiteDomainResponse response = service.registerDomain(
                USER_ID,
                SITE_ID,
                new RegisterDomainRequest("  Example.COM ")
        );

        assertThat(response.getHostname()).isEqualTo("example.com");
        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getSsl()).isEqualTo("pending");
        assertThat(response.getRecords())
                .extracting(DnsRecordResponse::getType)
                .containsExactly("TXT", "A", "A");
        assertThat(response.getRecords().get(0).getName())
                .isEqualTo("_sitebuilder-verify.example.com");
        assertThat(response.getRecords().get(0).getValue())
                .startsWith("sitebuilder-verification=");
        assertThat(response.getRecords().get(1).getValue()).isEqualTo("203.0.113.10");
        assertThat(response.getVerifiedAt()).isNull();
        assertThat(response.getLastCheckedAt()).isNull();

        verify(planLimitService).requireCustomDomainAllowed(USER_ID);
        verify(siteDomainRepository).save(any(SiteDomain.class));
    }

    @Test
    void registerUsesCnameForSubdomains() {
        SiteDomainResponse response = service.registerDomain(
                USER_ID,
                SITE_ID,
                new RegisterDomainRequest("www.example.com")
        );

        assertThat(response.getRecords())
                .extracting(DnsRecordResponse::getType)
                .containsExactly("TXT", "CNAME");
        assertThat(response.getRecords().get(1).getName()).isEqualTo("www.example.com");
        assertThat(response.getRecords().get(1).getValue()).isEqualTo("sites.example.net");
    }

    @Test
    void registerOmitsUnconfiguredRecords() {
        domainProperties.setARecords(List.of());

        SiteDomainResponse response = service.registerDomain(
                USER_ID,
                SITE_ID,
                new RegisterDomainRequest("example.com")
        );

        assertThat(response.getRecords())
                .extracting(DnsRecordResponse::getType)
                .containsExactly("TXT");
    }

    @Test
    void invalidHostnamesAreRejected() {
        List<String> invalidHostnames = List.of(
                "",
                "http://example.com",
                "https://example.com",
                "example.com:8080",
                "example.com/path",
                "exa mple.com",
                "*.example.com",
                "localhost",
                "192.168.1.1",
                "-bad.example.com",
                "bad-.example.com",
                "example",
                "exa_mple.com",
                "sitebuilder.example",
                "app.sitebuilder.example"
        );

        for (String hostname : invalidHostnames) {
            ApiException exception = assertThrows(
                    ApiException.class,
                    () -> service.registerDomain(
                            USER_ID,
                            SITE_ID,
                            new RegisterDomainRequest(hostname)
                    ),
                    hostname
            );

            assertThat(exception.getStatus()).as(hostname).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        verify(siteDomainRepository, never()).save(any());
    }

    @Test
    void registerReturnsConflictWhenHostnameBelongsToAnotherSite() {
        Site otherSite = Site.builder().id(999L).build();
        SiteDomain existing = SiteDomain.builder()
                .id(50L)
                .site(otherSite)
                .hostname("taken.com")
                .build();

        when(siteDomainRepository.findByHostname("taken.com")).thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.registerDomain(
                        USER_ID,
                        SITE_ID,
                        new RegisterDomainRequest("taken.com")
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(siteDomainRepository, never()).save(any());
    }

    @Test
    void otherUsersSiteIsReportedAsNotFound() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getDomain(USER_ID, SITE_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Site not found.");
        verifyNoInteractions(siteDomainRepository);
    }

    @Test
    void registerIsForbiddenWhenPlanDisallowsCustomDomains() {
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "Custom domains are not allowed."))
                .when(planLimitService)
                .requireCustomDomainAllowed(USER_ID);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.registerDomain(
                        USER_ID,
                        SITE_ID,
                        new RegisterDomainRequest("example.com")
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(siteDomainRepository, never()).save(any());
    }

    @Test
    void getReturnsUnconfiguredWhenNoDomainExists() {
        when(siteDomainRepository.findBySiteId(SITE_ID)).thenReturn(Optional.empty());

        SiteDomainResponse response = service.getDomain(USER_ID, SITE_ID);

        assertThat(response.getStatus()).isEqualTo("unconfigured");
        assertThat(response.getSsl()).isEqualTo("unconfigured");
        assertThat(response.getRecords()).isEmpty();
        assertThat(response.getHostname()).isNull();
    }

    @Test
    void verifyMarksDomainConnectedWhenTxtTokenMatches() {
        SiteDomain domain = pendingDomain();
        when(siteDomainRepository.findBySiteId(SITE_ID)).thenReturn(Optional.of(domain));
        when(dnsResolver.lookupTxt("_sitebuilder-verify.example.com"))
                .thenReturn(List.of("\"sitebuilder-verification=token-123\""));

        SiteDomainResponse response = service.verifyDomain(USER_ID, SITE_ID);

        assertThat(response.getStatus()).isEqualTo("connected");
        assertThat(response.getSsl()).isEqualTo("pending");
        assertThat(response.getVerifiedAt()).isNotNull();
        assertThat(response.getLastCheckedAt()).isNotNull();
        assertThat(response.getError()).isNull();
        assertThat(domain.getStatus()).isEqualTo(SiteDomainStatus.CONNECTED);
        assertThat(domain.getSslStatus()).isEqualTo(SiteDomainSslStatus.PENDING);

        verify(sslProvisioner).provision("example.com");
        verify(siteDomainRepository).save(domain);
    }

    @Test
    void verifyStaysPendingWhenTxtTokenDoesNotMatch() {
        SiteDomain domain = pendingDomain();
        when(siteDomainRepository.findBySiteId(SITE_ID)).thenReturn(Optional.of(domain));
        when(dnsResolver.lookupTxt("_sitebuilder-verify.example.com"))
                .thenReturn(List.of("\"sitebuilder-verification=other-token\""));

        SiteDomainResponse response = service.verifyDomain(USER_ID, SITE_ID);

        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getVerifiedAt()).isNull();
        assertThat(response.getLastCheckedAt()).isNotNull();
        assertThat(response.getError()).isNotBlank();

        verify(sslProvisioner, never()).provision(anyString());
        verify(siteDomainRepository).save(domain);
    }

    @Test
    void verifyStaysPendingWhenDnsLookupFails() {
        SiteDomain domain = pendingDomain();
        when(siteDomainRepository.findBySiteId(SITE_ID)).thenReturn(Optional.of(domain));
        when(dnsResolver.lookupTxt(anyString())).thenThrow(new DnsLookupException("timeout"));

        SiteDomainResponse response = service.verifyDomain(USER_ID, SITE_ID);

        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getStatus()).isNotEqualTo("error");
        assertThat(response.getVerifiedAt()).isNull();
        assertThat(response.getLastCheckedAt()).isNotNull();
        assertThat(response.getError()).isNotBlank();

        verify(sslProvisioner, never()).provision(anyString());
        verify(siteDomainRepository).save(domain);
    }

    @Test
    void publicLookupResolvesOnlyConnectedDomains() {
        SiteDomain connected = pendingDomain();
        connected.setStatus(SiteDomainStatus.CONNECTED);

        when(siteDomainRepository.findByHostname("example.com")).thenReturn(Optional.of(connected));

        assertThat(service.resolveConnectedSiteSlug("Example.COM")).isEqualTo("my-site");

        connected.setStatus(SiteDomainStatus.PENDING);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.resolveConnectedSiteSlug("example.com")
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private SiteDomain pendingDomain() {
        return SiteDomain.builder()
                .id(1L)
                .site(site)
                .hostname("example.com")
                .verificationToken("token-123")
                .status(SiteDomainStatus.PENDING)
                .sslStatus(SiteDomainSslStatus.PENDING)
                .build();
    }
}
