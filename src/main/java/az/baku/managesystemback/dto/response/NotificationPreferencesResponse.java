package az.baku.managesystemback.dto.response;

public record NotificationPreferencesResponse(
        boolean inApp,
        boolean email,
        boolean securityAlerts,
        boolean websiteUpdates,
        boolean billingUpdates
) {
}
