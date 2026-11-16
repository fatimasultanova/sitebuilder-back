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
import az.baku.managesystemback.event.PasswordResetEmailRequested;
import az.baku.managesystemback.event.VerificationEmailRequested;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.EmailVerificationTokenRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.util.RoleUtils;
import az.baku.managesystemback.util.TokenUtils;
import az.baku.managesystemback.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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


    @Transactional
    public LoginResponse loginWithGoogle(String email, String name, String googleSubject) {
        return loginWithGoogle("login", email, name, googleSubject);
    }

    @Transactional
    public LoginResponse loginWithGoogle(
            String rawIntent,
            String email,
            String name,
            String googleSubject
    ) {
        String intent = rawIntent == null || rawIntent.isBlank()
                ? "LOGIN"
                : rawIntent.trim().toUpperCase(Locale.ROOT);

        if (!intent.equals("LOGIN") && !intent.equals("REGISTER")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported Google authentication intent."
            );
        }

        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalized).orElse(null);

        if (intent.equals("REGISTER")) {
            if (user != null) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "An account with this email already exists."
                );
            }

            NameParts parts = splitName(
                    name == null || name.isBlank() ? "Google User" : name,
                    null
            );

            user = User.builder()
                    .name(parts.name())
                    .surname(parts.surname())
                    .email(normalized)
                    .password(passwordEncoder.encode(TokenUtils.generateToken()))
                    .role(RoleUtils.toStoredRole(RoleUtils.USER))
                    .enabled(true)
                    .emailVerified(true)
                    .googleSubject(googleSubject)
                    .build();

            try {
                user = userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "An account with this email or Google account already exists."
                );
            }
        } else {
            if (user == null) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        INVALID_CREDENTIALS_MESSAGE
                );
            }

            if (!user.isEnabled() || !user.isEmailVerified()) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "This account is not available for Google sign-in."
                );
            }

            if (user.getGoogleSubject() != null
                    && !user.getGoogleSubject().equals(googleSubject)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "A different Google account is already linked."
                );
            }

            user.setGoogleSubject(googleSubject);

            try {
                user = userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "This Google account is already linked to another user."
                );
            }
        }

        return new LoginResponse(
                createSession(user, null, "Google OAuth"),
                UserResponse.from(user)
        );
    }

    @Transactional
    public LoginResponse linkGoogleAccount(
            Long userId,
            String googleEmail,
            String googleSubject
    ) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "This account is not available for Google linking."
            );
        }

        if (googleEmail == null
                || !user.getEmail().equalsIgnoreCase(googleEmail.trim())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "The verified Google email must match this account's email."
            );
        }

        if (user.getGoogleSubject() != null
                && !user.getGoogleSubject().equals(googleSubject)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "A different Google account is already linked."
            );
        }

        user.setGoogleSubject(googleSubject);

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "This Google account is already linked to another user."
            );
        }

        String sessionToken = createSession(user, null, "Google account link");
        return new LoginResponse(sessionToken, UserResponse.from(user));
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        authSessionRepository.findByTokenHash(TokenUtils.sha256(rawToken.trim())).ifPresent(session -> {
            if (session.getRevokedAt() == null) session.setRevokedAt(Instant.now());
        });
    }

    @Transactional
    public String register(RegisterRequest request) {
        PasswordPolicy.validate(request.getPassword(), "password");
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

        String rawToken = createSession(user, ipAddress, userAgent);
        return new LoginResponse(rawToken, UserResponse.from(user));
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .ifPresent(user -> {
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
        PasswordPolicy.validate(request.getNewPassword(), "newPassword");
        PasswordPolicy.ensureNotSameAsCurrent(
                request.getNewPassword(), user.getPassword(), passwordEncoder, "newPassword");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.deleteAllByUser(user);
        authSessionRepository.deleteAllByUser(user);
    }

    @Transactional
    public String resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.getEmail());
        Instant now = Instant.now();

        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    if (isResendCooldownActive(user, now)) {
                        return;
                    }
                    issueVerificationToken(user);
                });

        return GENERIC_VERIFICATION_MESSAGE;
    }

    @Transactional
    public String verifyEmail(VerifyEmailRequest request) {
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
        userRepository.save(user);

        emailVerificationTokenRepository.deleteAllByUser(user);

        return "Email address verified successfully.";
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

    private String createSession(User user, String ipAddress, String userAgent) {
        String rawToken = TokenUtils.generateToken();
        Instant now = Instant.now();

        AuthSession session = AuthSession.builder()
                .tokenHash(TokenUtils.sha256(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(authProperties.getSessionTtl()))
                .lastUsedAt(now)
                .ipAddress(truncate(ipAddress, 45))
                .userAgent(truncate(userAgent, 512))
                .build();

        authSessionRepository.save(session);
        return rawToken;
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
}
