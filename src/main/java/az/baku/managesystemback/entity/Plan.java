package az.baku.managesystemback.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
/**
 * @author Fatima Sultanova
 */

@Entity
@Table(schema = "sitebuilder-data", name = "plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String name; // STARTER, PRO, AGENCY

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int maxSites; // 1, və ya -1 (limitsiz)

    @Column(nullable = false)
    private boolean customDomainAllowed;

    @Column(nullable = false)
    private boolean removeBranding;

    @Column(nullable = false)
    private boolean teamAllowed;
}
