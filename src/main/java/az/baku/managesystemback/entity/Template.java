package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Entity
@Table(schema = "sitebuilder-data", name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "description", columnDefinition = "text")
    @ColumnDefault("''")
    private String description;

    @Column(name = "preview_image_url")
    private String previewImageUrl;

    @Column(name = "premium", nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean premium = false;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private int sortOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_config", columnDefinition = "jsonb", nullable = false)
    @ColumnDefault("'{}'")
    private Map<String, Object> themeConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "translations", columnDefinition = "jsonb", nullable = false)
    @ColumnDefault("'{}'")
    private Map<String, Object> translations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structure", columnDefinition = "jsonb", nullable = false)
    @ColumnDefault("'{}'")
    private Map<String, Object> structure;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    @ColumnDefault("now()")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    @ColumnDefault("now()")
    private OffsetDateTime updatedAt;
}
