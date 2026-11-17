package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.UpdateNotificationPreferencesRequest;
import az.baku.managesystemback.dto.response.NotificationPreferencesResponse;
import az.baku.managesystemback.entity.NotificationPreferences;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationPreferencesServiceTest {

    private static final long USER_ID = 7L;

    private NotificationPreferencesRepository repository;
    private NotificationPreferencesService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationPreferencesRepository.class);
        service = new NotificationPreferencesService(repository);
    }

    @Test
    void createsDefaultsLazilyWhenNoRowExists() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferencesResponse response = service.getPreferences(USER_ID);

        assertThat(response.inApp()).isTrue();
        assertThat(response.email()).isFalse();
        assertThat(response.securityAlerts()).isTrue();
        assertThat(response.websiteUpdates()).isTrue();
        assertThat(response.billingUpdates()).isTrue();
    }

    @Test
    void rejectsDisablingSecurityAlerts() {
        UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                .securityAlerts(false)
                .build();

        assertThatThrownBy(() -> service.updatePreferences(USER_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getErrors())
                            .containsEntry("securityAlerts", List.of("Security alerts cannot be disabled."));
                });
    }

    @Test
    void updatesOnlyTheProvidedFields() {
        NotificationPreferences existing = NotificationPreferences.builder()
                .userId(USER_ID)
                .inApp(true)
                .email(false)
                .securityAlerts(true)
                .websiteUpdates(true)
                .billingUpdates(true)
                .build();

        when(repository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                .email(true)
                .billingUpdates(false)
                .build();

        NotificationPreferencesResponse response = service.updatePreferences(USER_ID, request);

        assertThat(response.inApp()).isTrue();
        assertThat(response.email()).isTrue();
        assertThat(response.securityAlerts()).isTrue();
        assertThat(response.websiteUpdates()).isTrue();
        assertThat(response.billingUpdates()).isFalse();
    }
}
