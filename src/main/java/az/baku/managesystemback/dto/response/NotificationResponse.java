package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String title,
        String message,
        String type,
        String category,
        Instant createdAt,
        boolean isRead,
        Instant readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                Long.toString(notification.getId()),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getCategory(),
                notification.getCreatedAt(),
                notification.getReadAt() != null,
                notification.getReadAt()
        );
    }
}
