package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findAllByUserIdOrderByCreatedAtDesc(long uid, Pageable p);

    Optional<SupportTicket> findByIdAndUserId(long id, long uid);
}
