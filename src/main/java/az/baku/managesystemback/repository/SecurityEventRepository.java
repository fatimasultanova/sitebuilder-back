package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findTop50ByUserIdOrderByOccurredAtDesc(long userId);

    Optional<SecurityEvent> findFirstByUserIdAndTypeOrderByOccurredAtDesc(long userId, String type);

    long countByUserIdAndTypeAndOccurredAtAfter(long userId, String type, Instant after);
}
