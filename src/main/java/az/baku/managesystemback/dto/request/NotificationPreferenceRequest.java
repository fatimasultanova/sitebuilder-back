package az.baku.managesystemback.dto.request;

public record NotificationPreferenceRequest(Boolean inApp, Boolean email, Boolean securityAlerts,
                                            Boolean websiteUpdates, Boolean billingUpdates) {
}
