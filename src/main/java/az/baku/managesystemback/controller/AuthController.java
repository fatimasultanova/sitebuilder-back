package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.ForgotPasswordRequest;
import az.baku.managesystemback.dto.request.GoogleOAuthStartRequest;
import az.baku.managesystemback.dto.request.GoogleOAuthCallbackRequest;
import az.baku.managesystemback.dto.response.OAuthStartResponse;
import az.baku.managesystemback.service.GoogleOAuthService;
import az.baku.managesystemback.dto.request.LoginRequest;
import az.baku.managesystemback.dto.request.RegisterRequest;
import az.baku.managesystemback.dto.request.ResendVerificationRequest;
import az.baku.managesystemback.dto.request.ResetPasswordRequest;
import az.baku.managesystemback.dto.request.VerifyEmailRequest;
import az.baku.managesystemback.dto.response.LoginResponse;
import az.baku.managesystemback.dto.response.MessageResponse;
import az.baku.managesystemback.service.AuthService;
import az.baku.managesystemback.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping({"/api/auth", "/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7) ? authorization.substring(7).trim() : null;
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/google/start")
    public OAuthStartResponse googleStart(@Valid @RequestBody GoogleOAuthStartRequest request) {
        if (request.intent() != null
                && !request.intent().equalsIgnoreCase("login")
                && !request.intent().equalsIgnoreCase("register")) {
            throw new az.baku.managesystemback.exception.ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Google OAuth start supports login or registration."
            );
        }
        return new OAuthStartResponse(
                googleOAuthService.start(request.intent(), request.returnTo(), null)
        );
    }

    @PostMapping("/oauth/google/callback")
    public LoginResponse googleCallback(@Valid @RequestBody GoogleOAuthCallbackRequest request) {
        return googleOAuthService.callback(request.code(), request.state());
    }

    @PostMapping("/oauth/google/link")
    @Operation(summary = "Start Google account linking")
    public OAuthStartResponse googleLink(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody GoogleOAuthStartRequest request
    ) {
        if (user == null) {
            throw new az.baku.managesystemback.exception.ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        if (request.intent() != null
                && !request.intent().equalsIgnoreCase("link")) {
            throw new az.baku.managesystemback.exception.ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Google account linking requires the link intent."
            );
        }

        return new OAuthStartResponse(
                googleOAuthService.start("link", request.returnTo(), user.id())
        );
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent
    ) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr(), userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return ResponseEntity.ok(new MessageResponse(authService.resendVerification(request)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(new MessageResponse(authService.verifyEmail(request)));
    }
}
