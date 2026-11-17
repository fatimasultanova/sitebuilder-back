package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    Optional<PaymentMethod> findByIdAndWorkspaceId(UUID id, long workspaceId);

    List<PaymentMethod> findByWorkspaceIdOrderByCreatedAtDesc(long workspaceId);

    @Query("select pm from PaymentMethod pm where pm.workspace.id = :workspaceId and pm.isDefault = true")
    Optional<PaymentMethod> findDefaultByWorkspaceId(@Param("workspaceId") long workspaceId);

    long countByWorkspaceId(long workspaceId);
}
