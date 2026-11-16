package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.ActivityEvent;
import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.repository.ActivityEventRepository;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class ActivityService {
    private final ActivityEventRepository events;
    private final SiteRepository sites;
    private final DeploymentRepository deployments;
    private final PlanLimitService plans;

    // Joins the mutation transaction, so rolled-back changes never leave events.
    @Transactional
    public void record(Site site, String type, String summary) {
        save(site, type, summary, site.getOwner().getName());
    }

    @Transactional
    public void recordSystem(Site site, String type, String summary) {
        save(site, type, summary, null);
    }

    private void save(Site site, String type, String summary, String actorName) {
        events.save(ActivityEvent.builder().ownerId(site.getOwner().getId())
                .siteId(site.getId()).targetLabel(site.getProjectName())
                .type(type).summary(summary).actorName(actorName).createdAt(Instant.now()).build());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(long ownerId, int page, int size) {
        return events.findByOwnerIdOrderByCreatedAtDescIdDesc(ownerId, PageRequest.of(page, size))
                .stream().map(event -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", String.valueOf(event.getId()));
                    row.put("type", event.getType());
                    row.put("summary", event.getSummary());
                    row.put("createdAt", event.getCreatedAt().toString());
                    if (event.getActorName() != null) {
                        row.put("actor", Map.of("id", String.valueOf(event.getOwnerId()), "name", event.getActorName()));
                    }
                    row.put("target", Map.of("type", "site", "id", String.valueOf(event.getSiteId()),
                            "label", event.getTargetLabel()));
                    return row;
                }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> usage(long ownerId) {
        return Map.of("websites", sites.countByOwnerId(ownerId),
                "deployments", deployments.countByOwnerId(ownerId), "reportedAt", Instant.now().toString());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> limits(long ownerId) {
        var plan = plans.getEffectivePlan(ownerId).plan();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("websites", plan.getMaxSites() < 0 ? null : plan.getMaxSites());
        result.put("planName", plan.getName());
        return result;
    }
}
