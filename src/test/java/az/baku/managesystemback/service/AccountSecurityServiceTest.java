package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.ChangePasswordRequest;
import az.baku.managesystemback.dto.response.AccountSessionResponse;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.event.PasswordChangedEmailRequested;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.SecurityEventRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountSecurityServiceTest {

    private static final long USER_ID = 42L;
    private static final UUID CURRENT_SESSION_ID = UUID.randomUUID();
    private static final String IP_ADDRESS = "1.2.3.4";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36";

    private UserRepository userRepository;
    private AuthSessionRepository authSessionRepository;
    private SecurityEventRepository securityEventRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private SecurityEventService securityEventService;
    private AuthService authService;
    private ApplicationEventPublisher eventPublisher;
    private AccountSecurityService service;

    private User user;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authSessionRepository = mock(AuthSessionRepository.class);
        securityEventRepository = mock(SecurityEventRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        securityEventService = mock(SecurityEventService.class);
        authService = mock(AuthService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new AccountSecurityService(
                userRepository,
                authSessionRepository,
                securityEventRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                securityEventService,
                authService,
                eventPublisher
        );

        user = User.builder()
                .id(USER_ID)
                .name("Test")
                .surname("User")
                .email("user@example.com")
                .password("encoded-password")
                .enabled(true)
                .emailVerified(true)
                .build();
        principal = new AuthenticatedUser(USER_ID, user.getEmail(), "ROLE_USER", CURRENT_SESSION_ID);
    }

    @Test
    void returnsTooManyRequestsBeforeCheckingThePassword() {
        when(securityEventRepository.countByUserIdAndTypeAndOccurredAtAfter(
                eq(USER_ID),
                eq(SecurityEventService.PASSWORD_CHANGE_FAILED),
                any(Instant.class)
        )).thenReturn(5L);

        ChangePasswordRequest request = new ChangePasswordRequest("current", "new-password");

        assertThatThrownBy(() -> service.changePassword(principal, request, IP_ADDRESS, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(apiException.getMessage()).isEqualTo("Too many attempts. Try again later.");
                });

        verify(userRepository, never()).findById(anyLong());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void wrongCurrentPasswordReturnsUnprocessableEntityAndRecordsFailure() {
        when(securityEventRepository.countByUserIdAndTypeAndOccurredAtAfter(
                eq(USER_ID),
                eq(SecurityEventService.PASSWORD_CHANGE_FAILED),
                any(Instant.class)
        )).thenReturn(0L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "new-password");

        assertThatThrownBy(() -> service.changePassword(principal, request, IP_ADDRESS, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getErrors())
                            .containsEntry("currentPassword", List.of("Current password is incorrect."));
                });

        verify(securityEventService).record(
                user,
                SecurityEventService.PASSWORD_CHANGE_FAILED,
                IP_ADDRESS,
                USER_AGENT,
                null,
                CURRENT_SESSION_ID
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsNewPasswordEqualToCurrentPassword() {
        when(securityEventRepository.countByUserIdAndTypeAndOccurredAtAfter(
                eq(USER_ID),
                eq(SecurityEventService.PASSWORD_CHANGE_FAILED),
                any(Instant.class)
        )).thenReturn(0L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-password")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("current", "new-password");

        assertThatThrownBy(() -> service.changePassword(principal, request, IP_ADDRESS, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getErrors()).containsKey("newPassword");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void successfulPasswordChangeRevokesOtherSessionsAndPublishesEmail() {
        when(securityEventRepository.countByUserIdAndTypeAndOccurredAtAfter(
                eq(USER_ID),
                eq(SecurityEventService.PASSWORD_CHANGE_FAILED),
                any(Instant.class)
        )).thenReturn(0L);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "encoded-password")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "encoded-password")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

        ChangePasswordRequest request = new ChangePasswordRequest("current", "new-password");

        service.changePassword(principal, request, IP_ADDRESS, USER_AGENT);

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
        verify(authSessionRepository).revokeOtherActiveSessions(eq(USER_ID), eq(CURRENT_SESSION_ID), any(Instant.class));
        verify(passwordResetTokenRepository).deleteAllByUser(user);
        verify(securityEventService).record(
                user,
                SecurityEventService.PASSWORD_CHANGED,
                IP_ADDRESS,
                USER_AGENT,
                null,
                CURRENT_SESSION_ID
        );
        verify(eventPublisher).publishEvent(any(PasswordChangedEmailRequested.class));
    }

    @Test
    void rejectsRevokingTheCurrentSession() {
        assertThatThrownBy(() -> service.revokeSession(
                principal,
                CURRENT_SESSION_ID,
                IP_ADDRESS,
                USER_AGENT
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getMessage()).isEqualTo("Use sign out to end the current session.");
                });

        verify(securityEventService, never()).record(any(), anyString(), any(), any(), any(), any());
    }

    @Test
    void listsOnlyActiveSessionsWithCurrentFlagAndParsedAgent() {
        UUID otherSessionId = UUID.randomUUID();
        Instant now = Instant.now();
        AuthSession current = AuthSession.builder()
                .publicId(CURRENT_SESSION_ID)
                .userAgent(USER_AGENT)
                .ipAddress(IP_ADDRESS)
                .createdAt(now.minusSeconds(60))
                .lastUsedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        AuthSession other = AuthSession.builder()
                .publicId(otherSessionId)
                .userAgent(USER_AGENT)
                .ipAddress(IP_ADDRESS)
                .createdAt(now.minusSeconds(120))
                .lastUsedAt(now.minusSeconds(30))
                .expiresAt(now.plusSeconds(3600))
                .build();

        when(authSessionRepository.findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
                eq(USER_ID),
                any(Instant.class)
        )).thenReturn(List.of(other, current));

        List<AccountSessionResponse> sessions = service.listSessions(principal);

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).id()).isEqualTo(CURRENT_SESSION_ID.toString());
        assertThat(sessions.get(0).isCurrent()).isTrue();
        assertThat(sessions.get(0).browser()).isEqualTo("Chrome");
        assertThat(sessions.get(0).os()).isEqualTo("Windows");
        assertThat(sessions.get(0).device()).isNull();
        assertThat(sessions.get(0).location()).isNull();
        assertThat(sessions.get(1).id()).isEqualTo(otherSessionId.toString());
        assertThat(sessions.get(1).isCurrent()).isFalse();
    }

    @Test
    void overviewMapsStatusAndLatestSignIn() {
        user.setEmailVerified(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(securityEventRepository.findFirstByUserIdAndTypeOrderByOccurredAtDesc(
                USER_ID,
                SecurityEventService.LOGIN
        )).thenReturn(Optional.empty());

        var response = service.getOverview(USER_ID);

        assertThat(response.account().displayName()).isEqualTo("Test User");
        assertThat(response.account().status()).isEqualTo("pending");
        assertThat(response.account().lastSignInAt()).isNull();
        assertThat(response.account().lastSignInDevice()).isNull();
        assertThat(response.emailVerification().verified()).isFalse();
        assertThat(response.connectedAccounts()).isEmpty();
    }
}
