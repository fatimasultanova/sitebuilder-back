package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSiteRequest;
import az.baku.managesystemback.entity.Plan;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.Template;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.TemplateRepository;
import az.baku.managesystemback.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 10L;
    private static final long SITE_ID = 100L;
    private static final long TEMPLATE_ID = 20L;

    private SiteRepository siteRepository;
    private TemplateRepository templateRepository;
    private UserRepository userRepository;
    private PlanLimitService planLimitService;
    private SiteServiceImpl service;
    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        siteRepository = mock(SiteRepository.class);
        templateRepository = mock(TemplateRepository.class);
        userRepository = mock(UserRepository.class);
        planLimitService = mock(PlanLimitService.class);
        service = new SiteServiceImpl(siteRepository, templateRepository, userRepository, planLimitService);

        owner = User.builder()
                .id(USER_ID)
                .name("Test")
                .email("test@example.com")
                .password("secret")
                .build();
        workspace = Workspace.builder().id(WORKSPACE_ID).owner(owner).name("Test Workspace").build();
    }

    @Test
    void createIsForbiddenWhenPlanSiteLimitIsReached() {
        stubCreateSite();
        stubEffectivePlan(1);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(1L);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createSite(USER_ID, createRequest())
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getMessage())
                .isEqualTo("Plan site limit reached. Upgrade your plan to create more sites.");
        assertThat(exception.getErrors().get("code")).containsExactly("PLAN_LIMIT_REACHED");
        assertThat(exception.getErrors().get("limit")).containsExactly("1");
        assertThat(exception.getErrors().get("planCode")).containsExactly("STARTER");
        verify(userRepository).findByIdForUpdate(USER_ID);
        verify(siteRepository, never()).save(any());
    }

    @Test
    void duplicateIsForbiddenWhenPlanSiteLimitIsReached() {
        Site source = Site.builder().id(SITE_ID).owner(owner).build();
        when(siteRepository.findByIdAndOwnerId(SITE_ID, USER_ID)).thenReturn(Optional.of(source));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
        stubEffectivePlan(1);
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(1L);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.duplicateSite(USER_ID, SITE_ID)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getErrors().get("code")).containsExactly("PLAN_LIMIT_REACHED");
        verify(siteRepository, never()).save(any());
    }

    @Test
    void createIsAllowedWhenPlanHasUnlimitedSites() {
        stubCreateSite();
        stubEffectivePlan(-1);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(100L);
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSite(USER_ID, createRequest());

        verify(siteRepository).save(any(Site.class));
    }

    @Test
    void createIsAllowedBelowPlanLimit() {
        stubCreateSite();
        stubEffectivePlan(2);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(owner));
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(1L);
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSite(USER_ID, createRequest());

        verify(siteRepository).save(any(Site.class));
    }

    private void stubCreateSite() {
        Template template = Template.builder().id(TEMPLATE_ID).name("Template").active(true).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(siteRepository.existsBySlug("my-site")).thenReturn(false);
    }

    private void stubEffectivePlan(int maxSites) {
        Plan plan = Plan.builder()
                .id(1L)
                .code("STARTER")
                .name("STARTER")
                .maxSites(maxSites)
                .active(true)
                .build();
        when(planLimitService.getEffectivePlan(USER_ID))
                .thenReturn(new EffectivePlan(workspace, plan, null));
    }

    private CreateSiteRequest createRequest() {
        return CreateSiteRequest.builder()
                .templateId(TEMPLATE_ID)
                .projectName("My Site")
                .slug("my-site")
                .content(Map.of("blocks", List.of()))
                .build();
    }
}
