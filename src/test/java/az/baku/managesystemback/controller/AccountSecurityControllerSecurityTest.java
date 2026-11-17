package az.baku.managesystemback.controller;

import az.baku.managesystemback.config.SecurityConfig;
import az.baku.managesystemback.dto.response.AccountSecurityResponse;
import az.baku.managesystemback.dto.response.AccountSessionResponse;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.security.RestAccessDeniedHandler;
import az.baku.managesystemback.security.RestAuthenticationEntryPoint;
import az.baku.managesystemback.service.AccountSecurityService;
import az.baku.managesystemback.service.AuthService;
import az.baku.managesystemback.service.NotificationPreferencesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import az.baku.managesystemback.service.GoogleOAuthService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AccountSecurityController.class,
        NotificationPreferencesController.class,
        AuthController.class
})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AccountSecurityControllerSecurityTest {

    private static final UUID SESSION_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountSecurityService accountSecurityService;

    @MockitoBean
    private NotificationPreferencesService notificationPreferencesService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean

    private GoogleOAuthService googleOAuthService;

    @MockitoBean

    private AuthSessionRepository authSessionRepository;

    @Test
    void accountSecurityEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/account/security")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account/security")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/account/security/sessions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/account/security/activity")).andExpect(status().isUnauthorized());
    }

    @Test
    void notificationPreferencesEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/notifications/preferences")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void otherAuthEndpointsStayPublic() throws Exception {
        when(authService.forgotPassword(any())).thenReturn("If an account exists, an email has been sent.");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void accountSecurityAllowsCorsForTheFrontendOrigin() throws Exception {
        mockMvc.perform(options("/account/security")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void authenticatedOverviewUsesThePrincipal() throws Exception {
        AccountSecurityResponse response = new AccountSecurityResponse(
                new AccountSecurityResponse.Account(
                        "Test User",
                        "user@example.com",
                        "active",
                        Instant.now(),
                        "Chrome on Windows"
                ),
                new AccountSecurityResponse.EmailVerification(true, null),
                List.of()
        );

        when(accountSecurityService.getOverview(42L)).thenReturn(response);

        AuthenticatedUser principal = new AuthenticatedUser(
                42L,
                "user@example.com",
                "ROLE_USER",
                SESSION_ID
        );

        mockMvc.perform(get("/account/security").with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.displayName").value("Test User"))
                .andExpect(jsonPath("$.account.lastSignInDevice").value("Chrome on Windows"))
                .andExpect(jsonPath("$.emailVerification.verified").value(true))
                .andExpect(jsonPath("$.connectedAccounts").isArray());
    }

    @Test
    void sessionsExposePublicIdAndCurrentFlag() throws Exception {
        UUID otherSessionId = UUID.randomUUID();
        when(accountSecurityService.listSessions(any())).thenReturn(List.of(
                new AccountSessionResponse(
                        otherSessionId.toString(),
                        "Chrome",
                        "Windows",
                        null,
                        null,
                        "1.2.3.4",
                        Instant.now(),
                        Instant.now(),
                        false
                )
        ));

        AuthenticatedUser principal = new AuthenticatedUser(
                42L,
                "user@example.com",
                "ROLE_USER",
                SESSION_ID
        );

        mockMvc.perform(get("/account/security/sessions").with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(otherSessionId.toString()))
                .andExpect(jsonPath("$[0].isCurrent").value(false))
                .andExpect(jsonPath("$[0].device").doesNotExist())
                .andExpect(jsonPath("$[0].location").doesNotExist());
    }
}
