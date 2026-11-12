package az.baku.managesystemback.controller;

import az.baku.managesystemback.config.SecurityConfig;
import az.baku.managesystemback.dto.response.PlanResponse;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.security.RestAccessDeniedHandler;
import az.baku.managesystemback.security.RestAuthenticationEntryPoint;
import az.baku.managesystemback.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingController.class)
@Import(SecurityConfig.class)
class BillingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

    @MockitoBean
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler accessDeniedHandler;

    @Test
    void plansListIsPublic() throws Exception {
        PlanResponse plan = new PlanResponse(
                "1",
                "STARTER",
                "STARTER",
                "Get started with a single website.",
                BigDecimal.ZERO,
                "AZN",
                1,
                true,
                false,
                new PlanResponse.Prices(
                        new PlanResponse.Price(BigDecimal.ZERO, "AZN"),
                        new PlanResponse.Price(BigDecimal.ZERO, "AZN")
                ),
                List.of(new PlanResponse.Feature("1 website", true))
        );

        when(billingService.listPlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].code").value("STARTER"))
                .andExpect(jsonPath("$[0].purchasable").value(true))
                .andExpect(jsonPath("$[0].prices.monthly.currency").value("AZN"));
    }
}
