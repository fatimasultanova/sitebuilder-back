package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.Plan;
import az.baku.managesystemback.entity.Subscription;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.repository.PlanRepository;
import az.baku.managesystemback.repository.SubscriptionRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanLimitServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 10L;

    private WorkspaceRepository workspaceRepository;
    private SubscriptionRepository subscriptionRepository;
    private PlanRepository planRepository;
    private PlanLimitServiceImpl service;
    private Workspace workspace;
    private Plan starter;
    private Plan pro;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        planRepository = mock(PlanRepository.class);
        service = new PlanLimitServiceImpl(workspaceRepository, subscriptionRepository, planRepository);

        workspace = Workspace.builder().id(WORKSPACE_ID).name("Test Workspace").build();
        starter = plan(1L, "STARTER", 1);
        pro = plan(2L, "PRO", 5);

        when(workspaceRepository.findFirstByOwnerIdOrderByIdAsc(USER_ID))
                .thenReturn(Optional.of(workspace));
        when(planRepository.findByCodeIgnoreCase("STARTER")).thenReturn(Optional.of(starter));
    }

    @Test
    void activeSubscriptionKeepsItsPlanUntilPeriodEnd() {
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(
                subscription("ACTIVE", LocalDateTime.now().plusDays(10), true)
        ));

        EffectivePlan effectivePlan = service.getEffectivePlan(USER_ID);

        assertThat(effectivePlan.isFree()).isFalse();
        assertThat(effectivePlan.subscription()).isNotNull();
        assertThat(effectivePlan.plan().getCode()).isEqualTo("PRO");
    }

    @Test
    void activeSubscriptionWithoutPeriodEndUsesItsPlan() {
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(
                subscription("ACTIVE", null, false)
        ));

        EffectivePlan effectivePlan = service.getEffectivePlan(USER_ID);

        assertThat(effectivePlan.isFree()).isFalse();
        assertThat(effectivePlan.plan().getCode()).isEqualTo("PRO");
    }

    @Test
    void canceledSubscriptionFallsBackToStarterBeforePeriodEnd() {
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(
                subscription("CANCELED", LocalDateTime.now().plusDays(10), true)
        ));

        EffectivePlan effectivePlan = service.getEffectivePlan(USER_ID);

        assertThat(effectivePlan.isFree()).isTrue();
        assertThat(effectivePlan.subscription()).isNull();
        assertThat(effectivePlan.plan().getCode()).isEqualTo("STARTER");
    }

    @Test
    void expiredActiveSubscriptionFallsBackToStarter() {
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(
                subscription("ACTIVE", LocalDateTime.now().minusDays(1), false)
        ));

        EffectivePlan effectivePlan = service.getEffectivePlan(USER_ID);

        assertThat(effectivePlan.isFree()).isTrue();
        assertThat(effectivePlan.plan().getCode()).isEqualTo("STARTER");
    }

    @Test
    void missingSubscriptionFallsBackToStarter() {
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());

        EffectivePlan effectivePlan = service.getEffectivePlan(USER_ID);

        assertThat(effectivePlan.isFree()).isTrue();
        assertThat(effectivePlan.plan()).isSameAs(starter);
    }

    private Subscription subscription(String status, LocalDateTime periodEnd, boolean cancelAtPeriodEnd) {
        return Subscription.builder()
                .workspace(workspace)
                .plan(pro)
                .status(status)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(cancelAtPeriodEnd)
                .build();
    }

    private Plan plan(long id, String code, int maxSites) {
        return Plan.builder()
                .id(id)
                .code(code)
                .name(code)
                .maxSites(maxSites)
                .active(true)
                .build();
    }
}
