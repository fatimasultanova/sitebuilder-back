package az.baku.managesystemback.entity;
import jakarta.persistence.*;
import lombok.*;
/**
 * @author Fatima Sultanova
 */
@Entity
@Table(name = "templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // e.g., Business, Portfolio, E-commerce

    @Column(nullable = false)
    private boolean isPremium;

    @Column(nullable = false)
    private boolean isPublished;

    // Şablonun ümumi dizayn və struktur konfiqurasiyası (JSON formatında)
    @Column(columnDefinition = "TEXT")
    private String themeConfig;
}
