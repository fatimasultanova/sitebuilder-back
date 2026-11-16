package az.baku.managesystemback.service;

import az.baku.managesystemback.config.GoogleOAuthProperties;
import az.baku.managesystemback.entity.OAuthState;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.OAuthStateRepository;
import az.baku.managesystemback.util.TokenUtils;
import az.baku.managesystemback.dto.response.LoginResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private GoogleOAuthProperties properties;

    @Mock
    private OAuthStateRepository oauthStateRepository;

    @Mock
    private AuthService authService;

    private RestClient.Builder restClientBuilder;

    private MockRestServiceServer mockServer;

    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        googleOAuthService = new GoogleOAuthService(
                properties,
                oauthStateRepository,
                authService,
                restClientBuilder
        );
    }

    // LOGIN: successfully starts Google OAuth authentication
    @Test
    void googleLoginStart_shouldSucceed() {

        when(properties.getClientId())
                .thenReturn("test-client-id");

        when(properties.getClientSecret())
                .thenReturn("test-client-secret");

        when(properties.getRedirectUri())
                .thenReturn("http://localhost:3000/auth/callback");

        ArgumentCaptor<OAuthState> stateCaptor =
                ArgumentCaptor.forClass(OAuthState.class);

        String authorizationUrl = googleOAuthService.start(
                "login",
                "/dashboard",
                null
        );

        assertNotNull(authorizationUrl);
        assertTrue(authorizationUrl.startsWith(
                "https://accounts.google.com/o/oauth2/v2/auth"
        ));

        var queryParams = UriComponentsBuilder
                .fromUriString(authorizationUrl)
                .build()
                .getQueryParams();

        assertEquals("test-client-id", queryParams.getFirst("client_id"));
        assertEquals(
                "http://localhost:3000/auth/callback",
                queryParams.getFirst("redirect_uri")
        );
        assertEquals("code", queryParams.getFirst("response_type"));

        String rawState = queryParams.getFirst("state");

        assertNotNull(rawState);
        assertFalse(rawState.isBlank());

        verify(oauthStateRepository).save(stateCaptor.capture());

        OAuthState savedState = stateCaptor.getValue();

        assertEquals(
                TokenUtils.sha256(rawState),
                savedState.getStateHash()
        );
        assertEquals("LOGIN", savedState.getIntent());
        assertEquals("/dashboard", savedState.getReturnTo());
        assertNull(savedState.getLinkedUserId());

        verifyNoInteractions(authService);
    }

    // REGISTER: successfully starts Google OAuth registration
    @Test
    void googleRegisterStart_shouldSucceed() {

        when(properties.getClientId())
                .thenReturn("test-client-id");

        when(properties.getClientSecret())
                .thenReturn("test-client-secret");

        when(properties.getRedirectUri())
                .thenReturn("http://localhost:3000/auth/callback");

        ArgumentCaptor<OAuthState> stateCaptor =
                ArgumentCaptor.forClass(OAuthState.class);

        String authorizationUrl = googleOAuthService.start(
                "register",
                "/register",
                null
        );

        assertNotNull(authorizationUrl);

        verify(oauthStateRepository).save(stateCaptor.capture());

        OAuthState savedState = stateCaptor.getValue();

        assertEquals("REGISTER", savedState.getIntent());
        assertEquals("/register", savedState.getReturnTo());
        assertNull(savedState.getLinkedUserId());

        verifyNoInteractions(authService);
    }

    // Google OAuth: returns 503 when client credentials are missing
    @Test
    void googleOAuthStart_shouldReturn503_whenCredentialsAreMissing() {

        when(properties.getClientId()).thenReturn("");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.start("login", "/", null)
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatus()
        );

        verifyNoInteractions(oauthStateRepository);
        verifyNoInteractions(authService);
    }

    // Google OAuth: returns 400 when the intent is unsupported
    @Test
    void googleOAuthStart_shouldReturn400_whenIntentIsInvalid() {

        when(properties.getClientId())
                .thenReturn("test-client-id");

        when(properties.getClientSecret())
                .thenReturn("test-client-secret");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.start(
                        "invalid-intent",
                        "/",
                        null
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatus()
        );

        verifyNoInteractions(oauthStateRepository);
        verifyNoInteractions(authService);
    }

    // LINK: returns 401 when the linked user ID is missing
    @Test
    void googleOAuthStart_shouldReturn401_whenLinkUserIdIsMissing() {

        when(properties.getClientId())
                .thenReturn("test-client-id");

        when(properties.getClientSecret())
                .thenReturn("test-client-secret");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.start(
                        "LINK",
                        "/settings",
                        null
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        verifyNoInteractions(oauthStateRepository);
        verifyNoInteractions(authService);
    }

    // Callback: returns 400 when the OAuth state does not exist
    @Test
    void googleOAuthCallback_shouldReturn400_whenStateIsInvalid() {

        String rawState = "invalid-test-state";

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-code",
                        rawState
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatus()
        );

        verify(oauthStateRepository)
                .findByStateHash(TokenUtils.sha256(rawState));

        verify(oauthStateRepository, never())
                .delete(any(OAuthState.class));

        verifyNoInteractions(authService);
    }

    // Callback: returns 400 and deletes the state when it has expired
    @Test
    void googleOAuthCallback_shouldReturn400AndDeleteState_whenStateIsExpired() {

        String rawState = "expired-test-state";

        OAuthState expiredState = OAuthState.builder()
                .id(1L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().minusSeconds(60))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(expiredState));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-code",
                        rawState
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatus()
        );

        verify(oauthStateRepository)
                .findByStateHash(TokenUtils.sha256(rawState));

        verify(oauthStateRepository).delete(expiredState);

        verifyNoInteractions(authService);
    }

    // Callback: successfully completes Google LOGIN
    @Test
    void googleOAuthCallback_shouldLoginSuccessfully_whenGoogleProfileIsValid() {

        String rawState = "valid-login-state";

        OAuthState validState = OAuthState.builder()
                .id(2L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        LoginResponse expectedResponse =
                new LoginResponse("test-token", null);

        when(authService.loginWithGoogle(
                eq("LOGIN"),
                eq("test@example.com"),
                eq("Test User"),
                eq("google-sub-123")
        )).thenReturn(expectedResponse);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "google-access-token",
                            "token_type": "Bearer"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        "Authorization",
                        "Bearer google-access-token"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "sub": "google-sub-123",
                            "email": "test@example.com",
                            "email_verified": true,
                            "name": "Test User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        LoginResponse actualResponse = googleOAuthService.callback(
                "test-authorization-code",
                rawState
        );

        assertNotNull(actualResponse);
        assertEquals("test-token", actualResponse.getToken());

        verify(oauthStateRepository).delete(validState);

        verify(authService).loginWithGoogle(
                "LOGIN",
                "test@example.com",
                "Test User",
                "google-sub-123"
        );

        verify(authService, never()).linkGoogleAccount(
                any(),
                any(),
                any()
        );

        mockServer.verify();
    }
    // Callback: successfully completes Google REGISTER
    @Test
    void googleOAuthCallback_shouldRegisterSuccessfully_whenGoogleProfileIsValid() {

        String rawState = "valid-register-state";

        OAuthState validState = OAuthState.builder()
                .id(3L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("REGISTER")
                .returnTo("/register")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        LoginResponse expectedResponse =
                new LoginResponse("register-test-token", null);

        when(authService.loginWithGoogle(
                eq("REGISTER"),
                eq("newuser@example.com"),
                eq("New User"),
                eq("google-register-sub")
        )).thenReturn(expectedResponse);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "register-access-token",
                            "token_type": "Bearer"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        "Authorization",
                        "Bearer register-access-token"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "sub": "google-register-sub",
                            "email": "newuser@example.com",
                            "email_verified": true,
                            "name": "New User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        LoginResponse actualResponse = googleOAuthService.callback(
                "register-authorization-code",
                rawState
        );

        assertNotNull(actualResponse);
        assertEquals("register-test-token", actualResponse.getToken());

        verify(oauthStateRepository).delete(validState);

        verify(authService).loginWithGoogle(
                "REGISTER",
                "newuser@example.com",
                "New User",
                "google-register-sub"
        );

        verify(authService, never()).linkGoogleAccount(
                any(),
                any(),
                any()
        );

        mockServer.verify();
    }
    // Callback: successfully links Google account to an existing user
    @Test
    void googleOAuthCallback_shouldLinkAccountSuccessfully_whenGoogleProfileIsValid() {

        String rawState = "valid-link-state";
        Long linkedUserId = 15L;

        OAuthState validState = OAuthState.builder()
                .id(4L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LINK")
                .linkedUserId(linkedUserId)
                .returnTo("/settings")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        LoginResponse expectedResponse =
                new LoginResponse("link-test-token", null);

        when(authService.linkGoogleAccount(
                eq(linkedUserId),
                eq("linked@example.com"),
                eq("google-link-sub")
        )).thenReturn(expectedResponse);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "link-access-token",
                            "token_type": "Bearer"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        "Authorization",
                        "Bearer link-access-token"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "sub": "google-link-sub",
                            "email": "linked@example.com",
                            "email_verified": true,
                            "name": "Linked User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        LoginResponse actualResponse = googleOAuthService.callback(
                "link-authorization-code",
                rawState
        );

        assertNotNull(actualResponse);
        assertEquals("link-test-token", actualResponse.getToken());

        verify(oauthStateRepository).delete(validState);

        verify(authService).linkGoogleAccount(
                linkedUserId,
                "linked@example.com",
                "google-link-sub"
        );

        verify(authService, never()).loginWithGoogle(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );

        mockServer.verify();
    }
    // Callback: returns 401 when Google token response has no access token
    @Test
    void googleOAuthCallback_shouldReturn401_whenAccessTokenIsMissing() {

        String rawState = "missing-access-token-state";

        OAuthState validState = OAuthState.builder()
                .id(5L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                            "token_type": "Bearer"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-authorization-code",
                        rawState
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatus()
        );

        verify(oauthStateRepository).delete(validState);

        verifyNoInteractions(authService);

        mockServer.verify();
    }
    // Callback: returns 401 when Google user profile is null
    @Test
    void googleOAuthCallback_shouldReturn401_whenProfileIsNull() {

        String rawState = "null-profile-state";

        OAuthState validState = OAuthState.builder()
                .id(6L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "valid-access-token"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andRespond(withSuccess(
                        "",
                        MediaType.APPLICATION_JSON
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-authorization-code",
                        rawState
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(oauthStateRepository).delete(validState);
        verifyNoInteractions(authService);

        mockServer.verify();
    }
    // Callback: returns 401 when Google email is not verified
    @Test
    void googleOAuthCallback_shouldReturn401_whenEmailIsNotVerified() {

        String rawState = "unverified-email-state";

        OAuthState validState = OAuthState.builder()
                .id(7L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "valid-access-token"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "sub": "google-sub-123",
                            "email": "test@example.com",
                            "email_verified": false,
                            "name": "Test User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-authorization-code",
                        rawState
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(oauthStateRepository).delete(validState);
        verifyNoInteractions(authService);

        mockServer.verify();
    }
    // Callback: returns 401 when Google profile has no email
    @Test
    void googleOAuthCallback_shouldReturn401_whenEmailIsMissing() {

        String rawState = "missing-email-state";

        OAuthState validState = OAuthState.builder()
                .id(8L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "valid-access-token"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "sub": "google-sub-123",
                            "email_verified": true,
                            "name": "Test User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-authorization-code",
                        rawState
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(oauthStateRepository).delete(validState);
        verifyNoInteractions(authService);

        mockServer.verify();
    }
    // Callback: returns 401 when Google profile has no subject
    @Test
    void googleOAuthCallback_shouldReturn401_whenSubjectIsMissing() {

        String rawState = "missing-sub-state";

        OAuthState validState = OAuthState.builder()
                .id(9L)
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plusSeconds(600))
                .intent("LOGIN")
                .returnTo("/")
                .build();

        when(oauthStateRepository.findByStateHash(
                eq(TokenUtils.sha256(rawState))
        )).thenReturn(Optional.of(validState));

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess(
                        """
                        {
                            "access_token": "valid-access-token"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(
                        "https://www.googleapis.com/oauth2/v3/userinfo"
                ))
                .andRespond(withSuccess(
                        """
                        {
                            "email": "test@example.com",
                            "email_verified": true,
                            "name": "Test User"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> googleOAuthService.callback(
                        "test-authorization-code",
                        rawState
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(oauthStateRepository).delete(validState);
        verifyNoInteractions(authService);

        mockServer.verify();
    }

}
