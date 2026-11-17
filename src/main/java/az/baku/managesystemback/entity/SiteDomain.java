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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
@Entity
@Table(
        schema = "sitebuilder-data",
        name = "site_domains",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_site_domains_site_id", columnNames = "site_id"),
                @UniqueConstraint(name = "ux_site_domains_hostname", columnNames = "hostname")
        },
        indexes = {
                @Index(name = "idx_site_domains_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(name = "hostname", nullable = false, length = 253)
    private String hostname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private SiteDomainStatus status = SiteDomainStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "ssl_status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private SiteDomainSslStatus sslStatus = SiteDomainSslStatus.PENDING;

    @Column(name = "verification_token", nullable = false, length = 128)
    private String verificationToken;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("now()")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("now()")
    private LocalDateTime updatedAt;
}
