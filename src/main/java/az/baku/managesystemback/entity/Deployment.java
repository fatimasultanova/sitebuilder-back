package az.baku.managesystemback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Entity
@Table(
        schema = "sitebuilder-data",
        name = "deployments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_deployments_site_version",
                        columnNames = {"site_id", "version"}
                )
        },
        indexes = {
                @Index(name = "idx_deployments_site_id", columnList = "site_id"),
                @Index(name = "idx_deployments_owner_id", columnList = "owner_id"),
                @Index(name = "idx_deployments_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'DRAFT'")
    @Builder.Default
    private DeploymentStatus status = DeploymentStatus.DRAFT;

    @Column(name = "published_url", length = 500)
    private String publishedUrl;

    @Column(name = "log_message", columnDefinition = "text")
    private String logMessage;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb")
    private Map<String, Object> snapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    @ColumnDefault("now()")
    private Instant createdAt;

    @Column(name = "started_at", columnDefinition = "timestamptz")
    private Instant startedAt;

    @Column(name = "finished_at", columnDefinition = "timestamptz")
    private Instant finishedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private long lockVersion = 0L;
}
