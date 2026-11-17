package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(schema = "sitebuilder-data", name = "notifications", indexes = {@Index(name = "idx_notifications_user_created", columnList = "user_id,created_at"), @Index(name = "idx_notifications_user_read", columnList = "user_id,read_at")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(length = 40)
    private String category;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 2000)
    private String message;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Column(name = "read_at")
    private Instant readAt;
}
