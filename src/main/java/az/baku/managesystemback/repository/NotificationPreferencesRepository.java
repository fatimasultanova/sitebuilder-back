package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
}
