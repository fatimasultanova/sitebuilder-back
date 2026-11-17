package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Entity
@Table(
        schema = "sitebuilder-data",
        name = "sites",
        indexes = {
                @Index(name = "idx_sites_owner_id", columnList = "owner_id"),
                @Index(name = "idx_sites_template_id", columnList = "template_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Template template;

    @Column(name = "project_name", nullable = false, length = 150)
    private String projectName;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'DRAFT'")
    @Builder.Default
    private SiteStatus status = SiteStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    @ColumnDefault("'{}'")
    private Map<String, Object> content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("now()")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("now()")
    private LocalDateTime updatedAt;
}
