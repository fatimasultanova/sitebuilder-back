package az.baku.managesystemback.controller;

import az.baku.managesystemback.config.SecurityConfig;
import az.baku.managesystemback.dto.response.PublicSiteResponse;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.security.RestAccessDeniedHandler;
import az.baku.managesystemback.security.RestAuthenticationEntryPoint;
import az.baku.managesystemback.service.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DeploymentController.class, PublicSiteController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class DeploymentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeploymentService deploymentService;

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

    @Test
    void publicSiteEndpointIsAccessibleWithoutAuthentication() throws Exception {
        PublicSiteResponse response = PublicSiteResponse.builder()
                .siteId("100")
                .slug("my-site")
                .projectName("My Site")
                .version(3)
                .publishedUrl("https://sites.example/my-site")
                .publishedAt("2026-01-01T10:00:00Z")
                .snapshot(Map.of("site", Map.of("slug", "my-site")))
                .build();

        when(deploymentService.getPublicLiveSite("my-site")).thenReturn(response);

        mockMvc.perform(get("/api/public/sites/my-site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value("100"))
                .andExpect(jsonPath("$.slug").value("my-site"))
                .andExpect(jsonPath("$.snapshot.site.slug").value("my-site"));
    }

    @Test
    void deploymentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/deployments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }
}
