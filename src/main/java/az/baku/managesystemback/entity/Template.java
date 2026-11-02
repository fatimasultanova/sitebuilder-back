package az.baku.managesystemback.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // e.g., Business, Portfolio, E-commerce

    @Column(columnDefinition = "TEXT")
    private String description;

    private String previewImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPublished = true;

    // Default design/structure configuration of the template
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_config", columnDefinition = "jsonb")
    private JsonNode themeConfig;
}
