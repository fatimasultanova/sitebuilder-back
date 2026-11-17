package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.*;
import az.baku.managesystemback.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class ActivityServiceTest {
    private final ActivityEventRepository events = mock(ActivityEventRepository.class);
    private final SiteRepository sites = mock(SiteRepository.class);
    private final DeploymentRepository deployments = mock(DeploymentRepository.class);
    private final PlanLimitService plans = mock(PlanLimitService.class);
    private final ActivityService service = new ActivityService(events, sites, deployments, plans);

    @Test void humanAndAutomaticActionsKeepTheirActorAndOwnership() {
        Site site = Site.builder().id(8).projectName("Test")
                .owner(User.builder().id(7).name("Owner").build()).build();
        service.record(site, "domain.changed", "Domain attached.");
        service.recordSystem(site, "site.published", "Published.");
        var captured = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(events, times(2)).save(captured.capture());
        var rows = captured.getAllValues();
        assertThat(rows).allMatch(event -> event.getOwnerId() == 7 && event.getSiteId() == 8);
        assertThat(rows.get(0).getActorName()).isEqualTo("Owner");
        assertThat(rows.get(1).getActorName()).isNull();
        when(events.findByOwnerIdOrderByCreatedAtDescIdDesc(eq(7L), any())).thenReturn(rows);
        var response = service.list(7, 0, 100);
        assertThat(response.get(0)).containsKey("actor");
        assertThat(response.get(1)).doesNotContainKey("actor");
    }

    @Test void usageOnlyReportsMeasuredOwnerCountsAndActualPlanLimit() {
        when(sites.countByOwnerId(7)).thenReturn(2L);
        when(deployments.countByOwnerId(7)).thenReturn(4L);
        when(plans.getEffectivePlan(7)).thenReturn(new EffectivePlan(null,
                Plan.builder().code("PRO").name("Professional").maxSites(5).build(), null));
        assertThat(service.usage(7)).containsEntry("websites", 2L).containsEntry("deployments", 4L)
                .doesNotContainKeys("storage", "apiRequests", "members");
        assertThat(service.limits(7)).containsEntry("websites", 5).containsEntry("planName", "Professional");
    }
}
