package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.UpdateNotificationPreferencesRequest;
import az.baku.managesystemback.dto.response.NotificationPreferencesResponse;
import az.baku.managesystemback.entity.NotificationPreferences;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPreferencesService {

    private static final String SECURITY_ALERTS_DISABLED_MESSAGE = "Security alerts cannot be disabled.";

    private final NotificationPreferencesRepository notificationPreferencesRepository;

    @Transactional
    public NotificationPreferencesResponse getPreferences(long userId) {
        return toResponse(getOrCreate(userId));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(
            long userId,
            UpdateNotificationPreferencesRequest request
    ) {
        if (Boolean.FALSE.equals(request.getSecurityAlerts())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    SECURITY_ALERTS_DISABLED_MESSAGE,
                    Map.of("securityAlerts", List.of(SECURITY_ALERTS_DISABLED_MESSAGE))
            );
        }

        NotificationPreferences preferences = getOrCreate(userId);

        if (request.getInApp() != null) {
            preferences.setInApp(request.getInApp());
        }
        if (request.getEmail() != null) {
            preferences.setEmail(request.getEmail());
        }
        if (request.getSecurityAlerts() != null) {
            preferences.setSecurityAlerts(request.getSecurityAlerts());
        }
        if (request.getWebsiteUpdates() != null) {
            preferences.setWebsiteUpdates(request.getWebsiteUpdates());
        }
        if (request.getBillingUpdates() != null) {
            preferences.setBillingUpdates(request.getBillingUpdates());
        }

        preferences.setUpdatedAt(Instant.now());

        return toResponse(notificationPreferencesRepository.save(preferences));
    }

    private NotificationPreferences getOrCreate(long userId) {
        return notificationPreferencesRepository.findById(userId)
                .orElseGet(() -> notificationPreferencesRepository.save(
                        NotificationPreferences.builder()
                                .userId(userId)
                                .updatedAt(Instant.now())
                                .build()
                ));
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(
                preferences.isInApp(),
                preferences.isEmail(),
                preferences.isSecurityAlerts(),
                preferences.isWebsiteUpdates(),
                preferences.isBillingUpdates()
        );
    }
}
