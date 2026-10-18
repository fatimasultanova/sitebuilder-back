package az.baku.managesystemback.entity;
import jakarta.persistence.*;
import lombok.*;
/**
 * @author Fatima Sultanova
 */
@Entity
@Table(name = "pages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug; // e.g., "/", "/about", "/contact"

    @Column(nullable = false)
    private boolean isHome;

    // Səhifənin daxilindəki bloklar və ya elementlərin JSON quruluşu (Redaktor taslağı)
    @Column(columnDefinition = "TEXT")
    private String contentJson;
}
