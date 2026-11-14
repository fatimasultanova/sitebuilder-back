package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(schema = "sitebuilder-data", name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @Column(name = "user_id")
    private long userId;

    @Builder.Default
    @Column(name = "in_app", nullable = false)
    private boolean inApp = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean email = false;

    @Builder.Default
    @Column(name = "security_alerts", nullable = false)
    private boolean securityAlerts = true;

    @Builder.Default
    @Column(name = "website_updates", nullable = false)
    private boolean websiteUpdates = true;

    @Builder.Default
    @Column(name = "billing_updates", nullable = false)
    private boolean billingUpdates = true;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
