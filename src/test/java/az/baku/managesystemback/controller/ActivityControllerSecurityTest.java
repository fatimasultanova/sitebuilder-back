package az.baku.managesystemback.controller;

import az.baku.managesystemback.config.SecurityConfig;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.security.*;
import az.baku.managesystemback.service.ActivityService;
import az.baku.managesystemback.service.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ActivityController.class, SitePublicationController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ActivityControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ActivityService activity;
    @MockitoBean DeploymentService deployments;
    @MockitoBean AuthSessionRepository sessions;

    @Test void anonymousAccountReadsAndWritesAreRejected() throws Exception {
        for (String path : List.of("/api/activity", "/api/usage", "/api/usage/limits", "/api/sites/1/publication")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(delete("/api/sites/1/publication")).andExpect(status().isUnauthorized());
        verifyNoInteractions(activity, deployments);
    }

    @Test void publicSnapshotCorsDoesNotGrantPrivateCors() throws Exception {
        mvc.perform(options("/api/public/sites/by-domain")
                .header("Origin", "https://customer.example")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/activity")
                .header("Origin", "https://customer.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test void accountComesFromPrincipalNotQueryParameter() throws Exception {
        var principal = new AuthenticatedUser(7L, "owner@example.com", "ROLE_USER");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(activity.list(7L, 0, 100)).thenReturn(List.of(Map.of("id", "1", "type", "site.created")));
        mvc.perform(get("/api/activity").param("ownerId", "99").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("site.created"));
        verify(activity).list(7L, 0, 100);
        mvc.perform(get("/api/activity").param("size", "201").with(authentication(auth)))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(activity);
    }
}
