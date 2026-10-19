package az.baku.managesystemback.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * @author Fatima Sultanova
 */
@Entity
@Table(schema = "sitebuilder-data", name = "sites")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template; // Hansı şablondan yaradılıb

    @Column(nullable = false)
    private String title;

    private String subdomain;

    private String customDomain;

    @Column(nullable = false)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, PUBLISHED

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}