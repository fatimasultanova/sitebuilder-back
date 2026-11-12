package az.baku.managesystemback.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
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

    @Column(nullable = false, unique = true, length = 32)
    private String code; // STARTER, PRO, AGENCY

    @Column(nullable = false, unique = true)
    private String name; // STARTER, PRO, AGENCY

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceYearly;

    @Column(nullable = false, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> features;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int maxSites; // 1, və ya -1 (limitsiz)

    @Column(nullable = false)
    private boolean customDomainAllowed;

    @Column(nullable = false)
    private boolean removeBranding;

    @Column(nullable = false)
    private boolean teamAllowed;
}
