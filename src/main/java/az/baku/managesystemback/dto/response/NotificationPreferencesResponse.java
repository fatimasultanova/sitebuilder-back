package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.NotificationPreference;

public record NotificationPreferencesResponse(boolean inApp, boolean email, boolean securityAlerts,
                                              boolean websiteUpdates, boolean billingUpdates) {
    public static NotificationPreferencesResponse from(NotificationPreference p) {
        return new NotificationPreferencesResponse(p.isInApp(), p.isEmail(), p.isSecurityAlerts(), p.isWebsiteUpdates(), p.isBillingUpdates());
    }
}
