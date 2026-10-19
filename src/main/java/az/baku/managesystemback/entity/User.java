package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(schema = "sitebuilder-data", name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Sistem daxilində ümumi rol (Məsələn: ROLE_USER, ROLE_ADMIN)
    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(nullable = false)
    private boolean enabled = true; // Hesab aktivdir ya yox

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
