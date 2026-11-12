package az.baku.managesystemback.service;

import az.baku.managesystemback.billing.FakePaymentProvider;
import az.baku.managesystemback.billing.PaymentProvider;
import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.dto.request.CheckoutRequest;
import az.baku.managesystemback.dto.request.ConfirmCheckoutRequest;
import az.baku.managesystemback.dto.response.BillingOverviewResponse;
import az.baku.managesystemback.dto.response.CheckoutConfirmResponse;
import az.baku.managesystemback.dto.response.CheckoutSessionResponse;
import az.baku.managesystemback.dto.response.PlanResponse;
import az.baku.managesystemback.entity.BillingCycle;
import az.baku.managesystemback.entity.Invoice;
import az.baku.managesystemback.entity.InvoiceStatus;
import az.baku.managesystemback.entity.Payment;
import az.baku.managesystemback.entity.PaymentMethod;
import az.baku.managesystemback.entity.PaymentSession;
import az.baku.managesystemback.entity.PaymentSessionStatus;
import az.baku.managesystemback.entity.PaymentStatus;
import az.baku.managesystemback.entity.Plan;
import az.baku.managesystemback.entity.Subscription;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.InvoiceRepository;
import az.baku.managesystemback.repository.PaymentMethodRepository;
import az.baku.managesystemback.repository.PaymentRepository;
import az.baku.managesystemback.repository.PaymentSessionRepository;
import az.baku.managesystemback.repository.PlanRepository;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.SubscriptionRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BillingServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 10L;

    private WorkspaceRepository workspaceRepository;
    private PlanRepository planRepository;
    private SubscriptionRepository subscriptionRepository;
    private PaymentSessionRepository paymentSessionRepository;
    private PaymentRepository paymentRepository;
    private InvoiceRepository invoiceRepository;
    private PaymentMethodRepository paymentMethodRepository;
    private SiteRepository siteRepository;
    private PlanLimitService planLimitService;
    private AppProperties appProperties;
    private BillingServiceImpl service;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        planRepository = mock(PlanRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        paymentSessionRepository = mock(PaymentSessionRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        paymentMethodRepository = mock(PaymentMethodRepository.class);
        siteRepository = mock(SiteRepository.class);

        planLimitService = new PlanLimitServiceImpl(
                workspaceRepository,
                subscriptionRepository,
                planRepository
        );
        appProperties = new AppProperties();
        appProperties.setFrontendUrl("http://localhost:3000");

        service = new BillingServiceImpl(
                workspaceRepository,
                planRepository,
                subscriptionRepository,
                paymentSessionRepository,
                paymentRepository,
                invoiceRepository,
                paymentMethodRepository,
                siteRepository,
                planLimitService,
                new FakePaymentProvider(),
                appProperties
        );
    }

    @Test
    void listPlansBuildsFeaturesFromLimitsAndMarksProRecommended() {
        Plan starter = plan(1L, "STARTER", 0, 0, 1, false, false, false, 1);
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        Plan agency = plan(3L, "AGENCY", 49, 490, -1, true, true, true, 3);

        when(planRepository.findByActiveTrueOrderByRankAsc()).thenReturn(List.of(starter, pro, agency));

        List<PlanResponse> plans = service.listPlans();

        assertThat(plans).hasSize(3);
        assertThat(plans.get(0).id()).isEqualTo("1");
        assertThat(plans.get(0).features().get(0).label()).isEqualTo("1 website");
        assertThat(plans.get(0).recommended()).isFalse();

        PlanResponse proResponse = plans.get(1);
        assertThat(proResponse.id()).isEqualTo("2");
        assertThat(proResponse.code()).isEqualTo("PRO");
        assertThat(proResponse.price()).isEqualByComparingTo("19.00");
        assertThat(proResponse.prices().monthly().amount()).isEqualByComparingTo("19.00");
        assertThat(proResponse.prices().annual().amount()).isEqualByComparingTo("190.00");
        assertThat(proResponse.purchasable()).isTrue();
        assertThat(proResponse.recommended()).isTrue();
        assertThat(proResponse.features())
                .extracting(PlanResponse.Feature::label)
                .containsExactly("5 websites", "Custom domain", "Remove branding", "Team access");

        assertThat(plans.get(2).features().get(0).label()).isEqualTo("Unlimited websites");
        assertThat(plans.get(2).features().get(3).included()).isTrue();
    }

    @Test
    void overviewWithoutSubscriptionReturnsFreeStarterPlan() {
        stubWorkspace();
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(2L);
        when(planRepository.findByCodeIgnoreCase("STARTER"))
                .thenReturn(Optional.of(plan(1L, "STARTER", 0, 0, 1, false, false, false, 1)));

        BillingOverviewResponse overview = service.getOverview(USER_ID);

        assertThat(overview.planId()).isEqualTo("1");
        assertThat(overview.planName()).isEqualTo("STARTER");
        assertThat(overview.status()).isEqualTo("free");
        assertThat(overview.cycle()).isEqualTo("monthly");
        assertThat(overview.price()).isEqualByComparingTo("0.00");
        assertThat(overview.currency()).isEqualTo("AZN");
        assertThat(overview.renewsAt()).isNull();
        assertThat(overview.cancelAtPeriodEnd()).isFalse();
        assertThat(overview.paymentMethodId()).isNull();
        assertThat(overview.maxSites()).isEqualTo(1);
        assertThat(overview.sitesUsed()).isEqualTo(2);
    }

    @Test
    void overviewUsesStarterLimitsForCanceledSubscriptionBeforePeriodEnd() {
        stubWorkspace();
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        Subscription canceled = Subscription.builder()
                .workspace(Workspace.builder().id(WORKSPACE_ID).name("Test Workspace").build())
                .plan(pro)
                .status("CANCELED")
                .currentPeriodEnd(LocalDateTime.now().plusDays(20))
                .cancelAtPeriodEnd(true)
                .build();

        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(canceled));
        when(siteRepository.countByOwnerId(USER_ID)).thenReturn(3L);
        when(planRepository.findByCodeIgnoreCase("STARTER"))
                .thenReturn(Optional.of(plan(1L, "STARTER", 0, 0, 1, false, false, false, 1)));

        BillingOverviewResponse overview = service.getOverview(USER_ID);

        assertThat(overview.planId()).isEqualTo("1");
        assertThat(overview.planName()).isEqualTo("STARTER");
        assertThat(overview.status()).isEqualTo("free");
        assertThat(overview.maxSites()).isEqualTo(1);
        assertThat(overview.sitesUsed()).isEqualTo(3);
        assertThat(overview.renewsAt()).isNull();
    }

    @Test
    void checkoutSessionUsesServerSideAnnualPriceAndReturnsFrontendUrl() {
        stubWorkspace();
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        UUID sessionId = UUID.randomUUID();

        when(planRepository.findByCodeIgnoreCase("PRO")).thenReturn(Optional.of(pro));
        when(paymentSessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });

        CheckoutSessionResponse response = service.createCheckoutSession(
                USER_ID,
                new CheckoutRequest("PRO", "annual")
        );

        assertThat(response.sessionId()).isEqualTo(sessionId.toString());
        assertThat(response.checkoutUrl())
                .isEqualTo("http://localhost:3000/billing/checkout/" + sessionId);
        assertThat(response.amount()).isEqualByComparingTo("190.00");
        assertThat(response.currency()).isEqualTo("AZN");
        assertThat(response.planName()).isEqualTo("PRO");
        assertThat(response.cycle()).isEqualTo("annual");

        ArgumentCaptor<PaymentSession> captor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(paymentSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentSessionStatus.PENDING);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("190.00");
        assertThat(captor.getValue().getBillingCycle()).isEqualTo(BillingCycle.YEARLY);
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
    }

    @Test
    void successfulConfirmActivatesSubscriptionAndCreatesPaymentInvoiceAndMethod() {
        stubWorkspace();
        UUID sessionId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        PaymentSession session = pendingSession(sessionId, pro, BillingCycle.MONTHLY, "19.00");

        when(paymentSessionRepository.findByIdAndWorkspaceId(sessionId, WORKSPACE_ID))
                .thenReturn(Optional.of(session));
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.countByInvoiceNumberStartingWith(anyString())).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(invoiceId);
            return invoice;
        });
        when(paymentMethodRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(0L);
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutConfirmResponse response = service.confirmCheckoutSession(
                USER_ID,
                sessionId,
                new ConfirmCheckoutRequest("4242424242424242", 12, 2030, "123", "Test User")
        );

        assertThat(response.status()).isEqualTo("paid");
        assertThat(response.amount()).isEqualByComparingTo("19.00");
        assertThat(response.invoiceId()).isEqualTo(invoiceId.toString());
        assertThat(response.invoiceNumber()).matches("INV-" + Year.now().getValue() + "-000001");
        assertThat(response.currentPeriodEnd()).isAfter(LocalDateTime.now().plusDays(27));
        assertThat(session.getStatus()).isEqualTo(PaymentSessionStatus.PAID);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(subscriptionCaptor.getValue().getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(subscriptionCaptor.getValue().isCancelAtPeriodEnd()).isFalse();
        assertThat(subscriptionCaptor.getValue().getCurrentPeriodEnd()).isNotNull();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo("fake");
        assertThat(paymentCaptor.getValue().getProviderReference()).startsWith("fake_");

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoiceCaptor.getValue().getAmount()).isEqualByComparingTo("19.00");

        ArgumentCaptor<PaymentMethod> methodCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(methodCaptor.capture());
        assertThat(methodCaptor.getValue().getLast4()).isEqualTo("4242");
        assertThat(methodCaptor.getValue().getBrand()).isEqualTo("Visa");
        assertThat(methodCaptor.getValue().getExpMonth()).isEqualTo(12);
        assertThat(methodCaptor.getValue().getExpYear()).isEqualTo(2030);
        assertThat(methodCaptor.getValue().isDefault()).isTrue();
    }

    @Test
    void declinedCardMarksSessionFailedWithoutCreatingPaymentOrInvoice() {
        stubWorkspace();
        UUID sessionId = UUID.randomUUID();
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        PaymentSession session = pendingSession(sessionId, pro, BillingCycle.MONTHLY, "19.00");

        when(paymentSessionRepository.findByIdAndWorkspaceId(sessionId, WORKSPACE_ID))
                .thenReturn(Optional.of(session));
        when(paymentSessionRepository.save(any(PaymentSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiException exception = assertThrows(ApiException.class, () -> service.confirmCheckoutSession(
                USER_ID,
                sessionId,
                new ConfirmCheckoutRequest("4000000000000002", 12, 2030, "123", "Test User")
        ));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(exception.getMessage()).isEqualTo("Card declined");
        assertThat(session.getStatus()).isEqualTo(PaymentSessionStatus.FAILED);
        assertThat(session.getFailureReason()).isEqualTo("Card declined");
        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void confirmIsIdempotentForAlreadyPaidSession() {
        PaymentProvider provider = mock(PaymentProvider.class);
        BillingServiceImpl idempotentService = new BillingServiceImpl(
                workspaceRepository,
                planRepository,
                subscriptionRepository,
                paymentSessionRepository,
                paymentRepository,
                invoiceRepository,
                paymentMethodRepository,
                siteRepository,
                planLimitService,
                provider,
                appProperties
        );

        stubWorkspace();
        UUID sessionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Plan pro = plan(2L, "PRO", 19, 190, 5, true, false, false, 2);
        PaymentSession session = pendingSession(sessionId, pro, BillingCycle.MONTHLY, "19.00");
        session.setStatus(PaymentSessionStatus.PAID);
        Payment payment = Payment.builder().id(paymentId).amount(new BigDecimal("19.00")).build();
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .invoiceNumber("INV-2026-000001")
                .amount(new BigDecimal("19.00"))
                .currency("AZN")
                .status(InvoiceStatus.PAID)
                .issuedAt(LocalDateTime.now())
                .build();
        Subscription subscription = Subscription.builder()
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        when(paymentSessionRepository.findByIdAndWorkspaceId(sessionId, WORKSPACE_ID))
                .thenReturn(Optional.of(session));
        when(paymentRepository.findFirstByPaymentSessionIdOrderByCreatedAtDesc(sessionId))
                .thenReturn(Optional.of(payment));
        when(invoiceRepository.findFirstByPaymentIdOrderByCreatedAtDesc(paymentId))
                .thenReturn(Optional.of(invoice));
        when(subscriptionRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(Optional.of(subscription));

        CheckoutConfirmResponse response = idempotentService.confirmCheckoutSession(
                USER_ID,
                sessionId,
                new ConfirmCheckoutRequest("4242424242424242", 12, 2030, "123", "Test User")
        );

        assertThat(response.status()).isEqualTo("paid");
        assertThat(response.invoiceId()).isEqualTo(invoiceId.toString());
        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-000001");
        verifyNoInteractions(provider);
        verify(paymentSessionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    void anotherWorkspacesSessionIsNotFound() {
        stubWorkspace();
        UUID sessionId = UUID.randomUUID();

        when(paymentSessionRepository.findByIdAndWorkspaceId(sessionId, WORKSPACE_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getCheckoutSession(USER_ID, sessionId)
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("Payment session not found.");
    }

    private void stubWorkspace() {
        Workspace workspace = Workspace.builder()
                .id(WORKSPACE_ID)
                .name("Test Workspace")
                .build();

        when(workspaceRepository.findFirstByOwnerIdOrderByIdAsc(USER_ID))
                .thenReturn(Optional.of(workspace));
    }

    private PaymentSession pendingSession(UUID sessionId, Plan plan, BillingCycle cycle, String amount) {
        return PaymentSession.builder()
                .id(sessionId)
                .workspace(Workspace.builder().id(WORKSPACE_ID).name("Test Workspace").build())
                .plan(plan)
                .billingCycle(cycle)
                .amount(new BigDecimal(amount))
                .currency("AZN")
                .status(PaymentSessionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    private Plan plan(
            long id,
            String code,
            int monthly,
            int yearly,
            int maxSites,
            boolean customDomainAllowed,
            boolean removeBranding,
            boolean teamAllowed,
            int rank
    ) {
        return Plan.builder()
                .id(id)
                .code(code)
                .name(code)
                .price(BigDecimal.valueOf(monthly))
                .priceMonthly(BigDecimal.valueOf(monthly))
                .priceYearly(BigDecimal.valueOf(yearly))
                .currency("AZN")
                .features(Map.of("raw", "ignored"))
                .rank(rank)
                .active(true)
                .maxSites(maxSites)
                .customDomainAllowed(customDomainAllowed)
                .removeBranding(removeBranding)
                .teamAllowed(teamAllowed)
                .build();
    }
}
