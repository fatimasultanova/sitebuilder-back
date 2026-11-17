package az.baku.managesystemback.service;

public interface PlanLimitService {

    EffectivePlan getEffectivePlan(long userId);

    void requireCustomDomainAllowed(long userId);
}
