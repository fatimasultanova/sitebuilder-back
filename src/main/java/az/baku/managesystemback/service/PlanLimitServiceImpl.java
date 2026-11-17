package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.Plan;
import az.baku.managesystemback.entity.Subscription;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.PlanRepository;
import az.baku.managesystemback.repository.SubscriptionRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanLimitServiceImpl implements PlanLimitService {

    private static final String STARTER_CODE = "STARTER";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String CUSTOM_DOMAIN_NOT_ALLOWED_MESSAGE =
            "Your plan does not include custom domains. Upgrade your plan to connect a domain.";

    private final WorkspaceRepository workspaceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    @Override
    public EffectivePlan getEffectivePlan(long userId) {
        Workspace workspace = workspaceRepository.findFirstByOwnerIdOrderByIdAsc(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace not found."));
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.findByWorkspaceId(workspace.getId())
                .filter(subscription -> isEffective(subscription, now))
                .map(subscription -> new EffectivePlan(workspace, subscription.getPlan(), subscription))
                .orElseGet(() -> new EffectivePlan(workspace, getStarterPlan(), null));
    }

    @Override
    public void requireCustomDomainAllowed(long userId) {
        Plan plan = getEffectivePlan(userId).plan();

        if (plan.isCustomDomainAllowed()) {
            return;
        }

        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("code", List.of("CUSTOM_DOMAIN_NOT_ALLOWED"));
        errors.put("planCode", List.of(plan.getCode()));

        throw new ApiException(
                HttpStatus.FORBIDDEN,
                CUSTOM_DOMAIN_NOT_ALLOWED_MESSAGE,
                errors
        );
    }

    private boolean isEffective(Subscription subscription, LocalDateTime now) {
        if (subscription.getStatus() == null
                || !ACTIVE_STATUS.equalsIgnoreCase(subscription.getStatus())) {
            return false;
        }

        return subscription.getCurrentPeriodEnd() == null
                || !subscription.getCurrentPeriodEnd().isBefore(now);
    }

    private Plan getStarterPlan() {
        return planRepository.findByCodeIgnoreCase(STARTER_CODE)
                .filter(Plan::isActive)
                .orElseGet(() -> planRepository.findByActiveTrueOrderByRankAsc()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active plans available.")));
    }
}
