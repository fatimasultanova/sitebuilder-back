package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.dto.request.CreateDeploymentRequest;
import az.baku.managesystemback.dto.response.DeploymentResponse;
import az.baku.managesystemback.dto.response.PublicSiteResponse;
import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.event.DeploymentRequestedEvent;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeploymentServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long SITE_ID = 100L;
    private static final long DEPLOYMENT_ID = 500L;

    private DeploymentRepository deploymentRepository;
    private SiteRepository siteRepository;
    private ApplicationEventPublisher eventPublisher;
    private AppProperties appProperties;
    private DeploymentServiceImpl service;

    private User owner;
    private Site site;

    @BeforeEach
    void setUp() {
        deploymentRepository = mock(DeploymentRepository.class);
        siteRepository = mock(SiteRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        appProperties = new AppProperties();
        appProperties.setPublishedUrlTemplate("https://sites.example/{slug}");

        service = new DeploymentServiceImpl(
                deploymentRepository,
                siteRepository,
                appProperties,
                eventPublisher,
                mock(ActivityService.class)
        );

        owner = User.builder()
                .id(USER_ID)
                .name("Test")
                .email("test@example.com")
                .password("secret")
                .build();

        Template template = Template.builder()
                .id(20L)
                .slug("saas-starter")
                .name("SaaS Starter")
                .category("saas")
                .build();

        site = Site.builder()
                .id(SITE_ID)
                .owner(owner)
                .template(template)
                .projectName("My Site")
                .slug("my-site")
                .status(SiteStatus.DRAFT)
                .content(Map.of("blocks", List.of()))
                .build();
    }

    @Test
    void createQueuesDeploymentAndPublishesAfterCommitEvent() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(deploymentRepository.existsBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(false);
        when(deploymentRepository.findMaxVersionBySiteId(SITE_ID)).thenReturn(2);
        when(deploymentRepository.existsBySiteIdAndVersion(SITE_ID, 3)).thenReturn(false);
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(invocation -> {
            Deployment deployment = invocation.getArgument(0);
            deployment.setId(DEPLOYMENT_ID);
            return deployment;
        });

        DeploymentResponse response = service.createDeployment(
                USER_ID,
                CreateDeploymentRequest.builder().siteId(SITE_ID).build()
        );

        assertThat(response.getId()).isEqualTo("500");
        assertThat(response.getSiteId()).isEqualTo("100");
        assertThat(response.getVersion()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("queued");
        assertThat(response.getPublishedUrl()).isNull();
        assertThat(response.getLogMessage()).isEqualTo("Deployment queued.");
        assertThat(response.getTriggeredBy()).isEqualTo("test@example.com");
        assertThat(response.getStartedAt()).isNull();
        assertThat(response.getFinishedAt()).isNull();

        ArgumentCaptor<DeploymentRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeploymentRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().deploymentId()).isEqualTo(DEPLOYMENT_ID);
    }

    @Test
    void createReturnsConflictWhenDeploymentIsAlreadyInProgress() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(deploymentRepository.existsBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createDeployment(
                        USER_ID,
                        CreateDeploymentRequest.builder().siteId(SITE_ID).build()
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(deploymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createReturnsConflictForDuplicateVersion() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(deploymentRepository.existsBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(false);
        when(deploymentRepository.existsBySiteIdAndVersion(SITE_ID, 2)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createDeployment(
                        USER_ID,
                        CreateDeploymentRequest.builder().siteId(SITE_ID).version(2).build()
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void otherUsersSiteIsReportedAsNotFound() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createDeployment(
                        USER_ID,
                        CreateDeploymentRequest.builder().siteId(SITE_ID).build()
                )
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Site not found.");
    }

    @Test
    void otherUsersDeploymentIsReportedAsNotFound() {
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getDeployment(USER_ID, DEPLOYMENT_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Deployment not found.");
    }

    @Test
    void listUsesLowercaseStatusAndNewestFirstFromRepository() {
        Deployment deployment = deployment(DeploymentStatus.LIVE);
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(deploymentRepository.findAllForOwner(USER_ID, SITE_ID, DeploymentStatus.LIVE))
                .thenReturn(List.of(deployment));

        List<DeploymentResponse> responses = service.listDeployments(USER_ID, SITE_ID, "live");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("live");
        verify(deploymentRepository).findAllForOwner(USER_ID, SITE_ID, DeploymentStatus.LIVE);
    }

    @Test
    void invalidStatusIsBadRequest() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.listDeployments(USER_ID, null, "unknown")
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void stopStopsOnlyQueuedOrBuildingDeployments() {
        Deployment queued = deployment(DeploymentStatus.QUEUED);
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(queued));
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentResponse response = service.stop(USER_ID, DEPLOYMENT_ID);

        assertThat(response.getStatus()).isEqualTo("stopped");
        assertThat(response.getFinishedAt()).isNotNull();
    }

    @Test
    void stopRejectsLiveDeployment() {
        Deployment live = deployment(DeploymentStatus.LIVE);
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(live));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.stop(USER_ID, DEPLOYMENT_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void redeployRejectsAnUnfinishedDeployment() {
        Deployment building = deployment(DeploymentStatus.BUILDING);
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(building));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.redeploy(USER_ID, DEPLOYMENT_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void redeployCreatesQueuedDeploymentFromCurrentSiteContent() {
        Deployment live = deployment(DeploymentStatus.LIVE);
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(live));
        when(deploymentRepository.existsBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(false);
        when(deploymentRepository.findMaxVersionBySiteId(SITE_ID)).thenReturn(4);
        when(deploymentRepository.existsBySiteIdAndVersion(SITE_ID, 5)).thenReturn(false);
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(invocation -> {
            Deployment deployment = invocation.getArgument(0);
            deployment.setId(501L);
            return deployment;
        });

        DeploymentResponse response = service.redeploy(USER_ID, DEPLOYMENT_ID);

        assertThat(response.getId()).isEqualTo("501");
        assertThat(response.getVersion()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo("queued");
    }

    @Test
    void rollbackMakesSupersededDeploymentLiveAndSupersedesCurrentLive() {
        Deployment target = deployment(DeploymentStatus.SUPERSEDED);
        Deployment currentLive = Deployment.builder()
                .id(600L)
                .site(site)
                .owner(owner)
                .version(6)
                .status(DeploymentStatus.LIVE)
                .build();

        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(target));
        when(deploymentRepository.findBySiteIdAndStatus(SITE_ID, DeploymentStatus.LIVE))
                .thenReturn(List.of(currentLive));
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentResponse response = service.rollback(USER_ID, DEPLOYMENT_ID);

        assertThat(response.getStatus()).isEqualTo("live");
        assertThat(response.getPublishedUrl()).isEqualTo("https://sites.example/my-site");
        assertThat(currentLive.getStatus()).isEqualTo(DeploymentStatus.SUPERSEDED);
        assertThat(site.getStatus()).isEqualTo(SiteStatus.PUBLISHED);
        verify(deploymentRepository).flush();
    }

    @Test
    void rollbackRejectsNonSupersededDeployment() {
        Deployment live = deployment(DeploymentStatus.LIVE);
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID)).thenReturn(Optional.of(live));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.rollback(USER_ID, DEPLOYMENT_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void publicLiveSiteReturnsLiveSnapshot() {
        Deployment live = deployment(DeploymentStatus.LIVE);
        live.setSnapshot(Map.of("site", Map.of("slug", "my-site")));
        when(deploymentRepository.findLiveBySiteSlug("my-site", DeploymentStatus.LIVE))
                .thenReturn(List.of(live));

        PublicSiteResponse response = service.getPublicLiveSite("My-Site");

        assertThat(response.getSiteId()).isEqualTo("100");
        assertThat(response.getSlug()).isEqualTo("my-site");
        assertThat(response.getSnapshot()).containsKey("site");
    }

    @Test
    void publicLiveSiteIsNotFoundWhenNoLiveDeploymentExists() {
        when(deploymentRepository.findLiveBySiteSlug("missing", DeploymentStatus.LIVE))
                .thenReturn(List.of());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getPublicLiveSite("missing")
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Deployment deployment(DeploymentStatus status) {
        return Deployment.builder()
                .id(DEPLOYMENT_ID)
                .site(site)
                .owner(owner)
                .version(5)
                .status(status)
                .build();
    }

    @Test
    void unpublishRetiresLiveAndCancelsPendingBuilds() {
        var live = deployment(DeploymentStatus.LIVE);
        var queued = deployment(DeploymentStatus.QUEUED);
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(site));
        when(deploymentRepository.findBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(List.of(live, queued));
        service.unpublish(USER_ID, SITE_ID);
        assertThat(live.getStatus()).isEqualTo(DeploymentStatus.SUPERSEDED);
        assertThat(queued.getStatus()).isEqualTo(DeploymentStatus.STOPPED);
        assertThat(queued.getFinishedAt()).isNotNull();
        assertThat(site.getStatus()).isEqualTo(SiteStatus.DRAFT);
        verify(siteRepository).lockForPublication(SITE_ID);
    }

    @Test
    void publicationOfAnotherOwnersSiteIsNotAccessible() {
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.empty());
        assertThat(assertThrows(ApiException.class, () -> service.getPublication(USER_ID, SITE_ID)).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThrows(ApiException.class, () -> service.unpublish(USER_ID, SITE_ID));
        verifyNoInteractions(deploymentRepository);
    }

    @Test
    void rollbackCannotCompeteWithAnInFlightBuild() {
        when(deploymentRepository.findByIdAndOwnerId(DEPLOYMENT_ID, USER_ID))
                .thenReturn(Optional.of(deployment(DeploymentStatus.SUPERSEDED)));
        when(deploymentRepository.existsBySiteIdAndStatusIn(anyLong(), anyCollection())).thenReturn(true);
        assertThat(assertThrows(ApiException.class, () -> service.rollback(USER_ID, DEPLOYMENT_ID)).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(deploymentRepository, never()).save(any());
    }
}
