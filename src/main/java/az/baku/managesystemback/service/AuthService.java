package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.dto.request.ForgotPasswordRequest;
import az.baku.managesystemback.dto.request.LoginRequest;
import az.baku.managesystemback.dto.request.RegisterRequest;
import az.baku.managesystemback.dto.request.ResendVerificationRequest;
import az.baku.managesystemback.dto.request.ResetPasswordRequest;
import az.baku.managesystemback.dto.request.VerifyEmailRequest;
import az.baku.managesystemback.dto.response.LoginResponse;
import az.baku.managesystemback.dto.response.UserResponse;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.EmailVerificationToken;
import az.baku.managesystemback.entity.PasswordResetToken;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.event.PasswordResetEmailRequested;
import az.baku.managesystemback.event.VerificationEmailRequested;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.EmailVerificationTokenRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import az.baku.managesystemback.util.RoleUtils;
import az.baku.managesystemback.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    private static final String INVALID_RESET_TOKEN_MESSAGE = "Password reset token is invalid or has expired.";
    private static final String INVALID_VERIFICATION_TOKEN_MESSAGE = "Verification token is invalid or has expired.";
    private static final String GENERIC_RESET_MESSAGE =
            "If an account with that email exists, a password reset link has been sent.";
    private static final String GENERIC_VERIFICATION_MESSAGE =
            "If an account with that email exists and is not verified, a verification email has been sent.";
    private static final String EMAIL_IN_USE_MESSAGE = "This email is already in use.";
    private static final String REGISTRATION_SUCCESS_MESSAGE =
            "Account created successfully. Please check your email to verify your account.";

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final SecurityEventService securityEventService;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public String register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw emailAlreadyInUse();
        }

        NameParts nameParts = splitName(request.getName(), request.getSurname());

        User user = User.builder()
                .name(nameParts.name())
                .surname(nameParts.surname())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleUtils.toStoredRole(RoleUtils.USER))
                .enabled(true)
                .emailVerified(false)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw emailAlreadyInUse();
        }

        ensureWorkspace(user);
        issueVerificationToken(user);

        return REGISTRATION_SUCCESS_MESSAGE;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = normalizeEmail(request.getEmail());
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        if (userOptional.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_PASSWORD_HASH);
            throw invalidCredentials();
        }

        User user = userOptional.get();

        if (!passwordMatches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }

        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled.");
        }

        if (!user.isEmailVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Email address has not been verified. Please check your inbox or request a new verification email."
            );
        }

        try {
            RoleUtils.normalize(user.getRole());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account role is not authorized.");
        }

        CreatedSession session = createSession(user, ipAddress, userAgent);
        securityEventService.record(
                user,
                SecurityEventService.LOGIN,
                ipAddress,
                userAgent,
                null,
                session.session().getPublicId()
        );
        return new LoginResponse(session.rawToken(), UserResponse.from(user));
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(email)
                .ifPresent(user -> {
                    securityEventService.record(
                            user,
                            SecurityEventService.RECOVERY_REQUESTED,
                            null,
                            null,
                            null,
                            null
                    );

                    if (!user.isEnabled()) {
                        return;
                    }

                    passwordResetTokenRepository.deleteAllByUser(user);

                    String rawToken = TokenUtils.generateToken();
                    Instant now = Instant.now();

                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .tokenHash(TokenUtils.sha256(rawToken))
                            .user(user)
                            .createdAt(now)
                            .expiresAt(now.plus(authProperties.getPasswordResetTokenTtl()))
                            .build();

                    passwordResetTokenRepository.save(resetToken);
                    eventPublisher.publishEvent(
                            new PasswordResetEmailRequested(user.getEmail(), displayName(user), rawToken)
                    );
                });

        return GENERIC_RESET_MESSAGE;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = TokenUtils.sha256(request.getToken().trim());
        Instant now = Instant.now();

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, INVALID_RESET_TOKEN_MESSAGE));

        if (!resetToken.isUsable(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_RESET_TOKEN_MESSAGE);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.deleteAllByUser(user);
        authSessionRepository.deleteAllByUser(user);

        securityEventService.record(
                user,
                SecurityEventService.PASSWORD_CHANGED,
                null,
                null,
                null,
                null
        );
    }

    @Transactional
    public String resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(email)
                .ifPresent(this::issueVerificationEmailIfAllowed);

        return GENERIC_VERIFICATION_MESSAGE;
    }

    @Transactional
    public String verifyEmail(VerifyEmailRequest request) {
        return verifyEmail(request, null, null);
    }

    @Transactional
    public String verifyEmail(VerifyEmailRequest request, String ipAddress, String userAgent) {
        String tokenHash = TokenUtils.sha256(request.getToken().trim());
        Instant now = Instant.now();

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, INVALID_VERIFICATION_TOKEN_MESSAGE));

        if (!verificationToken.isUsable(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_VERIFICATION_TOKEN_MESSAGE);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        emailVerificationTokenRepository.deleteAllByUser(user);

        securityEventService.record(
                user,
                SecurityEventService.EMAIL_VERIFIED,
                ipAddress,
                userAgent,
                null,
                null
        );

        return "Email address verified successfully.";
    }

    @Transactional
    public boolean issueVerificationEmailIfAllowed(User user) {
        if (user == null || !user.isEnabled() || user.isEmailVerified()) {
            return false;
        }

        Instant now = Instant.now();
        if (isResendCooldownActive(user, now)) {
            return false;
        }

        issueVerificationToken(user);
        return true;
    }

    @Transactional
    public void logout(long userId, UUID sessionPublicId, String ipAddress, String userAgent) {
        Optional<AuthSession> sessionOptional = sessionPublicId == null
                ? Optional.empty()
                : authSessionRepository.findByPublicIdAndUserId(sessionPublicId, userId);

        if (sessionOptional.isEmpty()) {
            return;
        }

        AuthSession session = sessionOptional.get();
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
            authSessionRepository.save(session);
        }

        securityEventService.record(
                session.getUser(),
                SecurityEventService.LOGOUT,
                ipAddress,
                userAgent,
                null,
                session.getPublicId()
        );
    }

    private void ensureWorkspace(User user) {
        if (workspaceRepository.findFirstByOwnerIdOrderByIdAsc(user.getId()).isPresent()) {
            return;
        }

        String baseName = user.getName();
        if (baseName == null || baseName.isBlank()) {
            String email = user.getEmail();
            int separator = email == null ? -1 : email.indexOf('@');
            baseName = separator > 0 ? email.substring(0, separator) : email;
        }

        String workspaceName = truncate((baseName == null ? "" : baseName.trim()) + "'s workspace", 255);
        workspaceRepository.save(Workspace.builder()
                .name(workspaceName)
                .owner(user)
                .build());
    }

    private void issueVerificationToken(User user) {
        emailVerificationTokenRepository.deleteAllByUser(user);

        String rawToken = TokenUtils.generateToken();
        Instant now = Instant.now();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .tokenHash(TokenUtils.sha256(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(authProperties.getEmailVerificationTokenTtl()))
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        eventPublisher.publishEvent(
                new VerificationEmailRequested(user.getEmail(), displayName(user), rawToken)
        );
    }

    private boolean isResendCooldownActive(User user, Instant now) {
        Duration cooldown = authProperties.getResendCooldown();
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            return false;
        }

        Instant cooldownStart = now.minus(cooldown);
        return emailVerificationTokenRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(EmailVerificationToken::getCreatedAt)
                .filter(createdAt -> createdAt != null && createdAt.isAfter(cooldownStart))
                .isPresent();
    }

    private String displayName(User user) {
        if (user.getSurname() == null || user.getSurname().isBlank()) {
            return user.getName();
        }
        return user.getName() + " " + user.getSurname();
    }

    private CreatedSession createSession(User user, String ipAddress, String userAgent) {
        String rawToken = TokenUtils.generateToken();
        Instant now = Instant.now();

        AuthSession session = AuthSession.builder()
                .publicId(UUID.randomUUID())
                .tokenHash(TokenUtils.sha256(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(authProperties.getSessionTtl()))
                .lastUsedAt(now)
                .ipAddress(truncate(ipAddress, 45))
                .userAgent(truncate(userAgent, 512))
                .build();

        authSessionRepository.save(session);
        return new CreatedSession(rawToken, session);
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private NameParts splitName(String rawName, String rawSurname) {
        String name = rawName == null ? "" : rawName.trim();
        String surname = rawSurname == null ? "" : rawSurname.trim();

        if (surname.isEmpty()) {
            int separator = name.indexOf(' ');
            if (separator > 0) {
                String combinedSurname = name.substring(separator + 1).trim();
                if (!combinedSurname.isEmpty()) {
                    surname = combinedSurname;
                    name = name.substring(0, separator).trim();
                }
            }
        }

        return new NameParts(name, surname.isEmpty() ? null : surname);
    }

    private ApiException emailAlreadyInUse() {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                EMAIL_IN_USE_MESSAGE,
                Map.of("email", List.of(EMAIL_IN_USE_MESSAGE))
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record NameParts(String name, String surname) {
    }

    private record CreatedSession(String rawToken, AuthSession session) {
    }
}
