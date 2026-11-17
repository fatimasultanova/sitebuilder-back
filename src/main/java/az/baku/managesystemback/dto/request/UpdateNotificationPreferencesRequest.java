package az.baku.managesystemback.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateNotificationPreferencesRequest {

    private Boolean inApp;
    private Boolean email;
    private Boolean securityAlerts;
    private Boolean websiteUpdates;
    private Boolean billingUpdates;
}
