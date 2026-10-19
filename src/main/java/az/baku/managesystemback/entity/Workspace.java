package az.baku.managesystemback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/**
 * @author Fatima Sultanova
 */

@Entity
@Table(schema = "sitebuilder-data", name = "workspaces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

}
