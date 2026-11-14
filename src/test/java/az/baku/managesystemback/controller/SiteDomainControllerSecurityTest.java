package az.baku.managesystemback.controller;

import az.baku.managesystemback.config.SecurityConfig;
import az.baku.managesystemback.dto.response.SiteDomainResponse;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.security.RestAccessDeniedHandler;
import az.baku.managesystemback.security.RestAuthenticationEntryPoint;
import az.baku.managesystemback.service.SiteDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteDomainController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SiteDomainControllerSecurityTest {

    private static final long USER_ID = 42L;
    private static final long SITE_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteDomainService siteDomainService;

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

    @Test
    void domainEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/sites/{siteId}/domain", SITE_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/sites/{siteId}/domain", SITE_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/sites/{siteId}/domain", SITE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hostname\":\"example.com\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/sites/{siteId}/domain/verify", SITE_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/sites/{siteId}/domain", SITE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void someoneElsesSiteIsReportedAsNotFound() throws Exception {
        when(siteDomainService.getDomain(USER_ID, SITE_ID))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Site not found."));

        mockMvc.perform(get("/api/sites/{siteId}/domain", SITE_ID)
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Site not found."));

        verify(siteDomainService).getDomain(USER_ID, SITE_ID);
    }

    @Test
    void authenticatedGetReturnsUnconfiguredDomainState() throws Exception {
        SiteDomainResponse response = SiteDomainResponse.builder()
                .hostname(null)
                .status("unconfigured")
                .ssl("unconfigured")
                .records(List.of())
                .verifiedAt(null)
                .lastCheckedAt(null)
                .error(null)
                .build();

        when(siteDomainService.getDomain(USER_ID, SITE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/sites/{siteId}/domain", SITE_ID)
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unconfigured"))
                .andExpect(jsonPath("$.ssl").value("unconfigured"))
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records").isEmpty());
    }

    private UsernamePasswordAuthenticationToken principalAuthentication() {
        AuthenticatedUser principal = new AuthenticatedUser(
                USER_ID,
                "user@example.com",
                "ROLE_USER"
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
