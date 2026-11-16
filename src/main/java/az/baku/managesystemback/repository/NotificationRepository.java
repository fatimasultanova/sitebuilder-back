package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(long uid, Pageable p);

    Page<Notification> findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(long uid, Pageable p);

    long countByUserIdAndReadAtIsNull(long uid);

    @Modifying
    @Query("update Notification n set n.readAt=current_timestamp where n.id=:id and n.user.id=:uid and n.readAt is null")
    int markRead(@Param("id") long id, @Param("uid") long uid);

    @Modifying
    @Query("update Notification n set n.readAt=current_timestamp where n.user.id=:uid and n.readAt is null")
    int markAllRead(@Param("uid") long uid);

    Page<Notification> findAllByUserIdAndCategoryOrderByCreatedAtDesc(
            long userId,
            String category,
            Pageable pageable
    );

    Page<Notification> findAllByUserIdAndCategoryAndReadAtIsNullOrderByCreatedAtDesc(
            long userId,
            String category,
            Pageable pageable
    );
}
