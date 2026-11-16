package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.ResendVerificationRequest;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.util.PasswordPolicy;
import az.baku.managesystemback.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountSecurityService {

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public AccountSecuritySummary getSummary(long userId) {
        User user = findUser(userId);

        long activeSessionCount = authSessionRepository
                .findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, Instant.now(), PageRequest.of(0, 100))
                .getTotalElements();

        return new AccountSecuritySummary(
                user.isEmailVerified(), user.getCreatedAt(), activeSessionCount);
    }

    @Transactional(readOnly = true)
    public Page<SessionSummary> getSessions(
            long userId, int page, int size, String currentBearerToken
    ) {
        String currentHash = hashOrEmpty(currentBearerToken);

        return authSessionRepository
                .findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, Instant.now(), PageRequest.of(page, size))
                .map(session -> SessionSummary.from(session, currentHash));
    }

    @Transactional
    public void revokeSession(long userId, long sessionId) {
        AuthSession session = authSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
        }
    }

    @Transactional
    public int revokeOtherSessions(long userId, String currentBearerToken) {
        String currentTokenHash = requireCurrentSessionHash(userId, currentBearerToken);
        return authSessionRepository.revokeOtherActiveSessions(
                userId, currentTokenHash, Instant.now());
    }

    @Transactional
    public void changePassword(
            long userId, String currentPassword, String newPassword, String currentBearerToken
    ) {
        User user = findUser(userId);
        String currentTokenHash = requireCurrentSessionHash(userId, currentBearerToken);

        if (!passwordMatches(currentPassword, user.getPassword())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation failed.",
                    Map.of("currentPassword", List.of("Current password is incorrect."))
            );
        }

        PasswordPolicy.validate(newPassword, "newPassword");
        PasswordPolicy.ensureNotSameAsCurrent(
                newPassword, user.getPassword(), passwordEncoder, "newPassword");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        authSessionRepository.revokeOtherActiveSessions(
                userId, currentTokenHash, Instant.now());

        notificationService.createForUser(
                userId, "account", "success",
                "Password changed", "Your account password was changed.");
    }

    @Transactional
    public String resendVerificationEmail(long userId) {
        User user = findUser(userId);
        return authService.resendVerification(new ResendVerificationRequest(user.getEmail()));
    }

    @Transactional(readOnly = true)
    public List<SecurityActivityEvent> getActivity(long userId) {
        return authSessionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
                .map(session -> new SecurityActivityEvent(
                        Long.toString(session.getId()),
                        "login",
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        session.getIpAddress(),
                        session.getUserAgent()))
                .getContent();
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String hashOrEmpty(String bearerToken) {
        return bearerToken == null || bearerToken.isBlank()
                ? ""
                : TokenUtils.sha256(bearerToken.trim());
    }

    private String requireCurrentSessionHash(long userId, String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "The current session token is required.");
        }

        String tokenHash = TokenUtils.sha256(bearerToken.trim());
        AuthSession currentSession = authSessionRepository
                .findActiveByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "The current session is invalid."));

        if (currentSession.getUser().getId() != userId) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "The current session is invalid.");
        }

        return tokenHash;
    }

    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
    public record AccountSecuritySummary(
            boolean emailVerified,
            java.time.LocalDateTime accountCreatedAt,
            long activeSessionCount
    ) {
    }

    public record SessionSummary(
            long id,
            Instant createdAt,
            Instant expiresAt,
            Instant lastUsedAt,
            Instant lastActiveAt,
            String ipAddress,
            String userAgent,
            boolean current
    ) {
        private static SessionSummary from(AuthSession session, String currentHash) {
            Instant active = session.getLastUsedAt() != null
                    ? session.getLastUsedAt()
                    : session.getCreatedAt();
            return new SessionSummary(
                    session.getId(),
                    session.getCreatedAt(),
                    session.getExpiresAt(),
                    session.getLastUsedAt(),
                    active,
                    session.getIpAddress(),
                    session.getUserAgent(),
                    session.getTokenHash().equals(currentHash));
        }
    }

    public record SecurityActivityEvent(
            String id,
            String type,
            Instant createdAt,
            Instant lastUsedAt,
            String ipAddress,
            String userAgent
    ) {
    }
}
