package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.Notification;
import az.baku.managesystemback.entity.NotificationPreferences;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import az.baku.managesystemback.repository.NotificationRepository;
import az.baku.managesystemback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationPreferencesRepository preferences;
    private final UserRepository users;

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "website", "publishing", "domain", "system", "account", "billing"
    );

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "info", "success", "warning", "error"
    );

    @Transactional(readOnly = true)
    public Page<Notification> list(
            long userId,
            int page,
            int size,
            boolean unread
    ) {
        return list(userId, page, size, unread, null);
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(
            long userId,
            int page,
            int size,
            boolean unread,
            String category
    ) {
        Pageable pageable = PageRequest.of(page, size);
        boolean hasCategory = category != null && !category.isBlank();

        if (hasCategory && unread) {
            return notifications.findAllByUserIdAndCategoryAndReadAtIsNullOrderByCreatedAtDesc(
                    userId,
                    category.trim().toLowerCase(Locale.ROOT),
                    pageable
            );
        }

        if (hasCategory) {
            return notifications.findAllByUserIdAndCategoryOrderByCreatedAtDesc(
                    userId,
                    category.trim().toLowerCase(Locale.ROOT),
                    pageable
            );
        }

        if (unread) {
            return notifications.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                    userId,
                    pageable
            );
        }

        return notifications.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unread(long uid) {
        return notifications.countByUserIdAndReadAtIsNull(uid);
    }

    @Transactional
    public void read(long uid, long id) {
        if (notifications.markRead(id, uid) == 0 && !notifications.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Notification not found.");
        }
    }

    @Transactional
    public void readAll(long uid) {
        notifications.markAllRead(uid);
    }

    @Transactional
    public void createForUser(
            long userId,
            String category,
            String type,
            String title,
            String message
    ) {
        String normalizedCategory = category == null
                ? ""
                : category.trim().toLowerCase(Locale.ROOT);

        String normalizedType = type == null
                ? ""
                : type.trim().toLowerCase(Locale.ROOT);

        if (!SUPPORTED_CATEGORIES.contains(normalizedCategory)) {
            normalizedCategory = "system";
        }

        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            normalizedType = "info";
        }

        String normalizedTitle = title == null || title.isBlank()
                ? "Notification"
                : title.trim();

        if (normalizedTitle.length() > 200) {
            normalizedTitle = normalizedTitle.substring(0, 200);
        }

        String normalizedMessage = message == null || message.isBlank()
                ? normalizedTitle
                : message.trim();

        if (normalizedMessage.length() > 2000) {
            normalizedMessage = normalizedMessage.substring(0, 2000);
        }

        User recipient = users.findById(userId).orElse(null);

        if (recipient == null) {
            return;
        }

        NotificationPreferences preference =
                preferences.findById(userId).orElse(null);

        if (preference != null && !preference.isInApp()) {
            return;
        }

        notifications.save(
                Notification.builder()
                        .user(recipient)
                        .category(normalizedCategory)
                        .type(normalizedType)
                        .title(normalizedTitle)
                        .message(normalizedMessage)
                        .build()
        );
    }
}
