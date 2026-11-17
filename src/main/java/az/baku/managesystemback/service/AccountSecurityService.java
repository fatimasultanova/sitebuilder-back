package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.ChangePasswordRequest;
import az.baku.managesystemback.dto.response.AccountSecurityResponse;
import az.baku.managesystemback.dto.response.AccountSessionResponse;
import az.baku.managesystemback.dto.response.SecurityActivityResponse;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.SecurityEvent;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.event.PasswordChangedEmailRequested;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.SecurityEventRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSecurityService {

    private static final String CURRENT_PASSWORD_INCORRECT_MESSAGE = "Current password is incorrect.";
    private static final String SAME_PASSWORD_MESSAGE =
            "New password must be different from the current password.";
    private static final String SESSION_NOT_FOUND_MESSAGE = "Session not found.";
    private static final String CURRENT_SESSION_MESSAGE = "Use sign out to end the current session.";
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "Too many attempts. Try again later.";
    private static final String VERIFICATION_EMAIL_MESSAGE =
            "If your email address is not verified, a verification email has been sent.";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration FAILED_ATTEMPTS_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final SecurityEventRepository securityEventRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventService securityEventService;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    public AccountSecurityResponse getOverview(long userId) {
        User user = getUser(userId);

        Optional<SecurityEvent> latestLogin = securityEventRepository
                .findFirstByUserIdAndTypeOrderByOccurredAtDesc(userId, SecurityEventService.LOGIN);

        Instant lastSignInAt = latestLogin.map(SecurityEvent::getOccurredAt).orElse(null);
        String lastSignInDevice = latestLogin
                .map(SecurityEvent::getUserAgent)
                .map(userAgent -> UserAgentParser.parse(userAgent).deviceLabel())
                .orElse(null);

        String status;
        if (!user.isEnabled()) {
            status = "disabled";
        } else if (!user.isEmailVerified()) {
            status = "pending";
        } else {
            status = "active";
        }

        AccountSecurityResponse.Account account = new AccountSecurityResponse.Account(
                displayName(user),
                user.getEmail(),
                status,
                lastSignInAt,
                lastSignInDevice
        );
        AccountSecurityResponse.EmailVerification emailVerification =
                new AccountSecurityResponse.EmailVerification(
                        user.isEmailVerified(),
                        user.getEmailVerifiedAt()
                );

        return new AccountSecurityResponse(account, emailVerification, List.of());
    }

    public List<AccountSessionResponse> listSessions(AuthenticatedUser principal) {
        Instant now = Instant.now();

        return authSessionRepository
                .findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(principal.id(), now)
                .stream()
                .sorted(Comparator
                        .comparing(
                                AuthSession::getLastUsedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(
                                AuthSession::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .map(session -> toSessionResponse(session, principal.sessionPublicId()))
                .toList();
    }

    @Transactional
    public void revokeSession(
            AuthenticatedUser principal,
            UUID sessionId,
            String ipAddress,
            String userAgent
    ) {
        if (sessionId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND_MESSAGE);
        }

        if (sessionId.equals(principal.sessionPublicId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CURRENT_SESSION_MESSAGE);
        }

        AuthSession session = authSessionRepository
                .findByPublicIdAndUserId(sessionId, principal.id())
                .filter(candidate -> candidate.getRevokedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND_MESSAGE));

        session.setRevokedAt(Instant.now());
        authSessionRepository.save(session);

        securityEventService.record(
                session.getUser(),
                SecurityEventService.SESSION_REVOKED,
                ipAddress,
                userAgent,
                "session revoked",
                session.getPublicId()
        );
    }

    @Transactional
    public void revokeOtherSessions(
            AuthenticatedUser principal,
            String ipAddress,
            String userAgent
    ) {
        User user = getUser(principal.id());

        authSessionRepository.revokeOtherActiveSessions(
                principal.id(),
                principal.sessionPublicId(),
                Instant.now()
        );

        securityEventService.record(
                user,
                SecurityEventService.SESSION_REVOKED,
                ipAddress,
                userAgent,
                "all other sessions",
                principal.sessionPublicId()
        );
    }

    @Transactional
    public void changePassword(
            AuthenticatedUser principal,
            ChangePasswordRequest request,
            String ipAddress,
            String userAgent
    ) {
        Instant now = Instant.now();

        long failedAttempts = securityEventRepository.countByUserIdAndTypeAndOccurredAtAfter(
                principal.id(),
                SecurityEventService.PASSWORD_CHANGE_FAILED,
                now.minus(FAILED_ATTEMPTS_WINDOW)
        );

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS_MESSAGE);
        }

        User user = getUser(principal.id());

        if (!passwordMatches(request.getCurrentPassword(), user.getPassword())) {
            securityEventService.record(
                    user,
                    SecurityEventService.PASSWORD_CHANGE_FAILED,
                    ipAddress,
                    userAgent,
                    null,
                    principal.sessionPublicId()
            );
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    CURRENT_PASSWORD_INCORRECT_MESSAGE,
                    Map.of("currentPassword", List.of(CURRENT_PASSWORD_INCORRECT_MESSAGE))
            );
        }

        if (passwordMatches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    SAME_PASSWORD_MESSAGE,
                    Map.of("newPassword", List.of(SAME_PASSWORD_MESSAGE))
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        authSessionRepository.revokeOtherActiveSessions(
                principal.id(),
                principal.sessionPublicId(),
                now
        );
        passwordResetTokenRepository.deleteAllByUser(user);

        securityEventService.record(
                user,
                SecurityEventService.PASSWORD_CHANGED,
                ipAddress,
                userAgent,
                null,
                principal.sessionPublicId()
        );

        eventPublisher.publishEvent(
                new PasswordChangedEmailRequested(user.getEmail(), displayName(user))
        );
    }

    @Transactional
    public String requestVerificationEmail(
            AuthenticatedUser principal,
            String ipAddress,
            String userAgent
    ) {
        User user = getUser(principal.id());

        boolean issued = authService.issueVerificationEmailIfAllowed(user);
        if (issued) {
            securityEventService.record(
                    user,
                    SecurityEventService.VERIFICATION_EMAIL_SENT,
                    ipAddress,
                    userAgent,
                    null,
                    principal.sessionPublicId()
            );
        }

        return VERIFICATION_EMAIL_MESSAGE;
    }

    public List<SecurityActivityResponse> getActivity(long userId) {
        return securityEventRepository
                .findTop50ByUserIdOrderByOccurredAtDesc(userId)
                .stream()
                .map(event -> new SecurityActivityResponse(
                        Long.toString(event.getId()),
                        event.getType(),
                        event.getOccurredAt(),
                        UserAgentParser.parse(event.getUserAgent()).deviceLabel(),
                        null,
                        event.getIpAddress(),
                        event.getDetail()
                ))
                .toList();
    }

    private AccountSessionResponse toSessionResponse(AuthSession session, UUID currentSessionPublicId) {
        UserAgentParser.UserAgentInfo agent = UserAgentParser.parse(session.getUserAgent());

        return new AccountSessionResponse(
                session.getPublicId().toString(),
                agent.browser(),
                agent.os(),
                null,
                null,
                session.getIpAddress(),
                session.getLastUsedAt() == null ? session.getCreatedAt() : session.getLastUsedAt(),
                session.getCreatedAt(),
                Objects.equals(session.getPublicId(), currentSessionPublicId)
        );
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));
    }

    private String displayName(User user) {
        if (user.getSurname() == null || user.getSurname().isBlank()) {
            return user.getName();
        }
        return user.getName() + " " + user.getSurname();
    }
}
