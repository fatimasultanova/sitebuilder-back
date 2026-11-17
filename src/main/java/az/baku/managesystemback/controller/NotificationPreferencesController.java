package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.UpdateNotificationPreferencesRequest;
import az.baku.managesystemback.dto.response.NotificationPreferencesResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.NotificationPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/notifications/preferences", "/notifications/preferences"})
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Per-user notification preferences")
public class NotificationPreferencesController {

    private final NotificationPreferencesService notificationPreferencesService;

    @GetMapping
    @Operation(summary = "Get the current account notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(notificationPreferencesService.getPreferences(principal.id()));
    }

    @PatchMapping
    @Operation(summary = "Update the current account notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        return ResponseEntity.ok(
                notificationPreferencesService.updatePreferences(principal.id(), request)
        );
    }
}
