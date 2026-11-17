package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.Plan;
import az.baku.managesystemback.entity.Subscription;
import az.baku.managesystemback.entity.Workspace;

public record EffectivePlan(Workspace workspace, Plan plan, Subscription subscription) {

    public boolean isFree() {
        return subscription == null;
    }
}
