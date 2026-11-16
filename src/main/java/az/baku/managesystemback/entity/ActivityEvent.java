package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Immutable account trail; target IDs survive deletion of the target site. */
@Entity
@Table(schema = "sitebuilder-data", name = "activity_events",
        indexes = @Index(name = "idx_activity_owner_created", columnList = "owner_id,created_at"))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "owner_id", nullable = false)
    private long ownerId;
    @Column(nullable = false, length = 64)
    private String type;
    @Column(name = "site_id", nullable = false)
    private long siteId;
    @Column(name = "target_label", nullable = false)
    private String targetLabel;
    @Column(nullable = false)
    private String summary;
    @Column(name = "actor_name")
    private String actorName;
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;
}
