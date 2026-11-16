package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(schema = "sitebuilder-data", name = "notification_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
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
}
