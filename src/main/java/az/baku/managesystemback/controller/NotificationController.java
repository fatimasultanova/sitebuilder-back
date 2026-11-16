package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.NotificationPreferenceRequest;
import az.baku.managesystemback.dto.response.NotificationResponse;
import az.baku.managesystemback.dto.response.NotificationPreferencesResponse;
import az.baku.managesystemback.entity.NotificationPreference;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/notifications", "/notifications"})
@RequiredArgsConstructor
@Validated
public class NotificationController {
    private final NotificationService s;

    @GetMapping
    @Operation(summary = "List current user's notifications")
    public Map<String, Object> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) String category
    ) {
        var result = s
                .list(user.id(), page, size, unread, category)
                .map(NotificationResponse::from);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", result.getContent());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());

        return response;
    }

    @GetMapping("/unread-count")
    public java.util.Map<String, Long> count(@AuthenticationPrincipal AuthenticatedUser u) {
        return java.util.Map.of("count", s.unread(u.id()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> read(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id) {
        s.read(u.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> all(@AuthenticationPrincipal AuthenticatedUser u) {
        s.readAll(u.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse prefs(@AuthenticationPrincipal AuthenticatedUser u) {
        return NotificationPreferencesResponse.from(s.prefs(u.id()));
    }

    @PatchMapping("/preferences")
    public NotificationPreferencesResponse update(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody NotificationPreferenceRequest r) {
        return NotificationPreferencesResponse.from(s.update(u.id(), r));
    }
}
