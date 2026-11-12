package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.PaymentSession;
import az.baku.managesystemback.entity.PaymentSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, UUID> {

    Optional<PaymentSession> findByIdAndWorkspaceId(UUID id, long workspaceId);

    List<PaymentSession> findByWorkspaceIdOrderByCreatedAtDesc(long workspaceId);

    List<PaymentSession> findByStatusAndExpiresAtBefore(PaymentSessionStatus status, LocalDateTime expiresAt);
}
