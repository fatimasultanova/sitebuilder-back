package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.ChangePasswordRequest;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.AccountSecurityService;
import az.baku.managesystemback.service.AccountSecurityService.AccountSecuritySummary;
import az.baku.managesystemback.service.AccountSecurityService.SessionSummary;
import az.baku.managesystemback.service.AccountSecurityService.SecurityActivityEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import az.baku.managesystemback.dto.response.MessageResponse;

import java.util.List;

@RestController
@RequestMapping({"/api/account/security", "/account/security"})
@RequiredArgsConstructor
@Tag(name = "Account Security")
public class AccountSecurityController {

    private final AccountSecurityService accountSecurityService;

    @GetMapping
    @Operation(summary = "Get account security overview")
    public AccountSecuritySummary getOverview(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return accountSecurityService.getSummary(user.id());
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active account sessions")
    public List<SessionSummary> getSessions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return accountSecurityService
                .getSessions(user.id(), 0, 100, bearerToken(authorization))
                .getContent();
    }

    @PostMapping("/password")
    @Operation(summary = "Change account password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountSecurityService.changePassword(
                user.id(),
                request.currentPassword(),
                request.newPassword(),
                bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke one account session")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long sessionId
    ) {
        accountSecurityService.revokeSession(user.id(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/revoke-others")
    @Operation(summary = "Revoke all other active sessions")
    public ResponseEntity<Void> revokeOtherSessions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        accountSecurityService.revokeOtherSessions(user.id(), bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private static String bearerToken(String authorization) {
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim()
                : "";
    }

    @PostMapping("/verification-email")
    @Operation(summary = "Resend account verification email")
    public ResponseEntity<MessageResponse> resendVerificationEmail(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String message = accountSecurityService.resendVerificationEmail(user.id());
        return ResponseEntity.ok(new MessageResponse(message));
    }
    @GetMapping("/activity")
    @Operation(summary = "List account security activity")
    public List<SecurityActivityEvent> getActivity(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return accountSecurityService.getActivity(user.id());
    }
}
