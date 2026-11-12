package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByWorkspaceId(long workspaceId);

    boolean existsByWorkspaceId(long workspaceId);
}
