package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.Notification;
import az.baku.managesystemback.entity.NotificationPreferences;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import az.baku.managesystemback.repository.NotificationRepository;
import az.baku.managesystemback.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notifications;
    private NotificationPreferencesRepository preferences;
    private UserRepository users;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepository.class);
        preferences = mock(NotificationPreferencesRepository.class);
        users = mock(UserRepository.class);

        service = new NotificationService(
                notifications,
                preferences,
                users
        );
    }

    @Test
    void createForUserUnknownCategoryShouldFallbackToSystem() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        when(preferences.findById(1L))
                .thenReturn(Optional.empty());

        when(notifications.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createForUser(
                1L,
                "unknown-category",
                "info",
                "Test title",
                "Test message"
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notifications).save(captor.capture());

        assertThat(captor.getValue().getCategory())
                .isEqualTo("system");
    }

    @Test
    void createForUserLongTitleShouldBeTruncatedTo200Characters() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        when(preferences.findById(1L))
                .thenReturn(Optional.empty());

        when(notifications.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String longTitle = "A".repeat(250);

        service.createForUser(
                1L,
                "system",
                "info",
                longTitle,
                "Test message"
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notifications).save(captor.capture());

        assertThat(captor.getValue().getTitle())
                .hasSize(200);
    }

    @Test
    void createForUserShouldNotSaveWhenInAppIsFalse() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        NotificationPreferences preference =
                mock(NotificationPreferences.class);

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        when(preferences.findById(1L))
                .thenReturn(Optional.of(preference));

        when(preference.isInApp())
                .thenReturn(false);

        service.createForUser(
                1L,
                "system",
                "info",
                "Test title",
                "Test message"
        );

        verify(notifications, never())
                .save(any(Notification.class));
    }

    @Test
    void createForUserMissingUserShouldNotThrowAndShouldNotSave() {
        when(users.findById(999L))
                .thenReturn(Optional.empty());

        assertThatCode(() ->
                service.createForUser(
                        999L,
                        "system",
                        "info",
                        "Test title",
                        "Test message"
                )
        ).doesNotThrowAnyException();

        verify(notifications, never())
                .save(any(Notification.class));
    }
}
