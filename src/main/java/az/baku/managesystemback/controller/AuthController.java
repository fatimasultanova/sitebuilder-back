package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.ForgotPasswordRequest;
import az.baku.managesystemback.dto.request.LoginRequest;
import az.baku.managesystemback.dto.request.RegisterRequest;
import az.baku.managesystemback.dto.request.ResendVerificationRequest;
import az.baku.managesystemback.dto.request.ResetPasswordRequest;
import az.baku.managesystemback.dto.request.VerifyEmailRequest;
import az.baku.managesystemback.dto.response.AuthResponse;
import az.baku.managesystemback.dto.response.LoginResponse;
import az.baku.managesystemback.dto.response.MessageResponse;
import az.baku.managesystemback.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
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
