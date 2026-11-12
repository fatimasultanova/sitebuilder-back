package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndWorkspaceId(UUID id, long workspaceId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    long countByInvoiceNumberStartingWith(String prefix);

    Optional<Invoice> findFirstByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    List<Invoice> findByWorkspaceIdOrderByCreatedAtDesc(long workspaceId);
}
