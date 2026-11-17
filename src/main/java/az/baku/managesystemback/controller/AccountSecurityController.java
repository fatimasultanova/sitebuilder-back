package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.ChangePasswordRequest;
import az.baku.managesystemback.dto.response.AccountSecurityResponse;
import az.baku.managesystemback.dto.response.AccountSessionResponse;
import az.baku.managesystemback.dto.response.MessageResponse;
import az.baku.managesystemback.dto.response.SecurityActivityResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.AccountSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/account/security", "/account/security"})
@RequiredArgsConstructor
@Tag(name = "Account Security", description = "Account overview, sessions, password and activity")
public class AccountSecurityController {

    private final AccountSecurityService accountSecurityService;

    @GetMapping
    @Operation(summary = "Get the current account security overview")
    public ResponseEntity<AccountSecurityResponse> getOverview(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(accountSecurityService.getOverview(principal.id()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions for the current account")
    public ResponseEntity<List<AccountSessionResponse>> listSessions(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(accountSecurityService.listSessions(principal));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke one of the current account's sessions")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        accountSecurityService.revokeSession(
                principal,
                sessionId,
                httpRequest.getRemoteAddr(),
                userAgent
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/revoke-others")
    @Operation(summary = "Revoke all other active sessions")
    public ResponseEntity<Void> revokeOtherSessions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        accountSecurityService.revokeOtherSessions(
                principal,
                httpRequest.getRemoteAddr(),
                userAgent
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    @Operation(summary = "Change the current account password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        accountSecurityService.changePassword(
                principal,
                request,
                httpRequest.getRemoteAddr(),
                userAgent
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verification-email")
    @Operation(summary = "Request a new verification email")
    public ResponseEntity<MessageResponse> requestVerificationEmail(
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        return ResponseEntity.ok(new MessageResponse(
                accountSecurityService.requestVerificationEmail(
                        principal,
                        httpRequest.getRemoteAddr(),
                        userAgent
                )
        ));
    }

    @GetMapping("/activity")
    @Operation(summary = "List the latest security activity for the current account")
    public ResponseEntity<List<SecurityActivityResponse>> getActivity(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(accountSecurityService.getActivity(principal.id()));
    }
}
