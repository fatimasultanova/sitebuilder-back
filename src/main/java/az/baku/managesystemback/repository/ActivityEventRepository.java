package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    List<ActivityEvent> findByOwnerIdOrderByCreatedAtDescIdDesc(long ownerId, Pageable pageable);
}
