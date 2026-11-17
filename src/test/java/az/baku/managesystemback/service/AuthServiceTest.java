package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.dto.response.LoginResponse;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.EmailVerificationTokenRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // No shared stubbing required
    }

    // LOGIN: successful authentication with an existing user
    @Test
    void googleLogin_shouldSucceed_whenUserExists() {
        when(authProperties.getSessionTtl())
                .thenReturn(Duration.ofDays(7));

        User user = createUser(
                1L,
                "test@example.com",
                null
        );

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.loginWithGoogle(
                "login",
                "test@example.com",
                "Test User",
                "google-sub-123"
        );

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());

        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());

        assertEquals("google-sub-123", user.getGoogleSubject());

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(userRepository).saveAndFlush(user);
        verify(authSessionRepository).save(any(AuthSession.class));

        verify(userRepository, never()).save(any(User.class));
    }

    // LOGIN: should return 401 when the user does not exist
    @Test
    void googleLogin_shouldReturn401_whenUserDoesNotExist() {

        when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.loginWithGoogle(
                        "login",
                        "unknown@example.com",
                        "Unknown User",
                        "google-sub-456"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(userRepository).findByEmailIgnoreCase("unknown@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(authSessionRepository, never()).save(any(AuthSession.class));
    }

    // REGISTER: successful registration with a new user

    @Test
    void googleRegister_shouldSucceed_whenUserDoesNotExist() {
        when(authProperties.getSessionTtl())
                .thenReturn(Duration.ofDays(7));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(userRepository.findByEmailIgnoreCase("new@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.loginWithGoogle(
                "register",
                "new@example.com",
                "New User",
                "google-sub-789"
        );

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());

        assertNotNull(response.getUser());
        assertEquals("new@example.com", response.getUser().getEmail());

        verify(userRepository).findByEmailIgnoreCase("new@example.com");

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getEmail().equals("new@example.com")
                        && user.getGoogleSubject().equals("google-sub-789")
                        && user.isEnabled()
                        && user.isEmailVerified()
        ));

        verify(passwordEncoder).encode(anyString());
        verify(authSessionRepository).save(any(AuthSession.class));
    }

    // REGISTER: should return 409 when the email already exists
    @Test
    void googleRegister_shouldReturn409_whenEmailAlreadyExists() {

        User existingUser = createUser(
                2L,
                "existing@example.com",
                "existing-google-sub"
        );

        when(userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> authService.loginWithGoogle(
                        "register",
                        "existing@example.com",
                        "Existing User",
                        "another-google-sub"
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(userRepository).findByEmailIgnoreCase("existing@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(authSessionRepository, never()).save(any(AuthSession.class));
    }

    // Creates a user object for testing
    private User createUser(
            Long id,
            String email,
            String googleSubject
    ) {
        return User.builder()
                .id(id)
                .name("Test")
                .surname("User")
                .email(email)
                .password("encoded-password")
                .role("ROLE_USER")
                .enabled(true)
                .emailVerified(true)
                .googleSubject(googleSubject)
                .build();
    }
}
