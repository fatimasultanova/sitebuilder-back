package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndWorkspaceId(UUID id, long workspaceId);

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByWorkspaceIdOrderByCreatedAtDesc(long workspaceId);

    List<Payment> findByPaymentSessionId(UUID paymentSessionId);

    Optional<Payment> findFirstByPaymentSessionIdOrderByCreatedAtDesc(UUID paymentSessionId);
}
