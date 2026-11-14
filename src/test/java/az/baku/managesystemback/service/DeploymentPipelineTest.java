package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeploymentPipelineTest {

    private static final long DEPLOYMENT_ID = 500L;
    private static final long SITE_ID = 100L;

    private DeploymentRepository deploymentRepository;
    private SiteRepository siteRepository;
    private DeploymentPipeline pipeline;

    private User owner;
    private Site site;

    @BeforeEach
    void setUp() {
        deploymentRepository = mock(DeploymentRepository.class);
        siteRepository = mock(SiteRepository.class);

        AppProperties appProperties = new AppProperties();
        appProperties.setPublishedUrlTemplate("https://sites.example/{slug}");

        pipeline = new DeploymentPipeline(
                deploymentRepository,
                siteRepository,
                appProperties,
                noOpTransactionManager()
        );

        owner = User.builder().id(1L).email("test@example.com").build();
        Template template = Template.builder()
                .id(20L)
                .slug("saas-starter")
                .name("SaaS Starter")
                .category("saas")
                .themeConfig(Map.of("themePreset", "saas"))
                .translations(Map.of())
                .structure(Map.of())
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
    void successfulPipelineSnapshotsSiteAndActivatesDeployment() {
        Deployment deployment = queuedDeployment();
        Deployment previousLive = Deployment.builder()
                .id(400L)
                .site(site)
                .owner(owner)
                .version(1)
                .status(DeploymentStatus.LIVE)
                .build();

        when(deploymentRepository.findById(DEPLOYMENT_ID)).thenReturn(Optional.of(deployment));
        when(deploymentRepository.findWithSiteAndTemplateById(DEPLOYMENT_ID))
                .thenReturn(Optional.of(deployment));
        when(deploymentRepository.findBySiteIdAndStatus(SITE_ID, DeploymentStatus.LIVE))
                .thenReturn(List.of(previousLive));
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pipeline.process(DEPLOYMENT_ID);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.LIVE);
        assertThat(deployment.getStartedAt()).isNotNull();
        assertThat(deployment.getFinishedAt()).isNotNull();
        assertThat(deployment.getPublishedUrl()).isEqualTo("https://sites.example/my-site");
        assertThat(deployment.getSnapshot()).containsKeys("site", "template", "capturedAt");
        assertThat(previousLive.getStatus()).isEqualTo(DeploymentStatus.SUPERSEDED);
        assertThat(site.getStatus()).isEqualTo(SiteStatus.PUBLISHED);
        verify(deploymentRepository).flush();
    }

    @Test
    void failedPipelineMarksDeploymentFailedAndLeavesPreviousLiveUntouched() {
        Deployment deployment = queuedDeployment();
        site.setContent(null);

        Deployment previousLive = Deployment.builder()
                .id(400L)
                .site(site)
                .owner(owner)
                .version(1)
                .status(DeploymentStatus.LIVE)
                .build();

        when(deploymentRepository.findById(DEPLOYMENT_ID)).thenReturn(Optional.of(deployment));
        when(deploymentRepository.findWithSiteAndTemplateById(DEPLOYMENT_ID))
                .thenReturn(Optional.of(deployment));
        when(deploymentRepository.findBySiteIdAndStatus(SITE_ID, DeploymentStatus.LIVE))
                .thenReturn(List.of(previousLive));
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pipeline.process(DEPLOYMENT_ID);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.FAILED);
        assertThat(deployment.getLogMessage()).isEqualTo("Deployment failed. Please try again.");
        assertThat(deployment.getFinishedAt()).isNotNull();
        assertThat(previousLive.getStatus()).isEqualTo(DeploymentStatus.LIVE);
        assertThat(site.getStatus()).isEqualTo(SiteStatus.DRAFT);
    }

    @Test
    void stoppedQueuedDeploymentIsNotProcessed() {
        Deployment deployment = queuedDeployment();
        deployment.setStatus(DeploymentStatus.STOPPED);

        when(deploymentRepository.findById(DEPLOYMENT_ID)).thenReturn(Optional.of(deployment));

        pipeline.process(DEPLOYMENT_ID);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.STOPPED);
        assertThat(deployment.getStartedAt()).isNull();
    }

    private Deployment queuedDeployment() {
        return Deployment.builder()
                .id(DEPLOYMENT_ID)
                .site(site)
                .owner(owner)
                .version(2)
                .status(DeploymentStatus.QUEUED)
                .build();
    }

    private PlatformTransactionManager noOpTransactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
