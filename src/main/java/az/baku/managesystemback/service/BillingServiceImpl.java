package az.baku.managesystemback.service;

import az.baku.managesystemback.billing.PaymentProvider;
import az.baku.managesystemback.billing.PaymentRequest;
import az.baku.managesystemback.billing.PaymentResult;
import az.baku.managesystemback.config.AppProperties;
import az.baku.managesystemback.dto.request.CancelSubscriptionRequest;
import az.baku.managesystemback.dto.request.CheckoutRequest;
import az.baku.managesystemback.dto.request.ConfirmCheckoutRequest;
import az.baku.managesystemback.dto.response.BillingOverviewResponse;
import az.baku.managesystemback.dto.response.CheckoutConfirmResponse;
import az.baku.managesystemback.dto.response.CheckoutSessionDetailsResponse;
import az.baku.managesystemback.dto.response.CheckoutSessionResponse;
import az.baku.managesystemback.dto.response.InvoiceResponse;
import az.baku.managesystemback.dto.response.PaymentMethodResponse;
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
import az.baku.managesystemback.mapper.BillingMapper;
import az.baku.managesystemback.repository.InvoiceRepository;
import az.baku.managesystemback.repository.PaymentMethodRepository;
import az.baku.managesystemback.repository.PaymentRepository;
import az.baku.managesystemback.repository.PaymentSessionRepository;
import az.baku.managesystemback.repository.PlanRepository;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.SubscriptionRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int SESSION_TTL_MINUTES = 30;
    private static final int FAILURE_REASON_MAX_LENGTH = 500;

    private final WorkspaceRepository workspaceRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SiteRepository siteRepository;
    private final PlanLimitService planLimitService;
    private final PaymentProvider paymentProvider;
    private final AppProperties appProperties;

    @Override
    public List<PlanResponse> listPlans() {
        return planRepository.findByActiveTrueOrderByRankAsc()
                .stream()
                .map(BillingMapper::toPlanResponse)
                .toList();
    }

    @Override
    public BillingOverviewResponse getOverview(long userId) {
        EffectivePlan effectivePlan = planLimitService.getEffectivePlan(userId);
        long sitesUsed = siteRepository.countByOwnerId(userId);

        if (effectivePlan.isFree()) {
            return freeOverview(effectivePlan.plan(), sitesUsed);
        }

        Subscription subscription = effectivePlan.subscription();
        Plan plan = effectivePlan.plan();
        BillingCycle cycle = subscription.getBillingCycle() == null
                ? BillingCycle.MONTHLY
                : subscription.getBillingCycle();
        String paymentMethodId = paymentMethodRepository.findDefaultByWorkspaceId(effectivePlan.workspace().getId())
                .map(paymentMethod -> String.valueOf(paymentMethod.getId()))
                .orElse(null);

        return BillingMapper.toOverview(
                plan,
                overviewStatus(subscription.getStatus()),
                cycle,
                priceFor(plan, cycle),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                paymentMethodId,
                sitesUsed
        );
    }

    @Override
    public List<InvoiceResponse> listInvoices(long userId) {
        Workspace workspace = getWorkspace(userId);

        return invoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(invoice -> BillingMapper.toInvoiceResponse(invoice, planName(invoice)))
                .toList();
    }

    @Override
    public InvoiceResponse getInvoiceReceipt(long userId, UUID invoiceId) {
        Workspace workspace = getWorkspace(userId);
        Invoice invoice = invoiceRepository.findByIdAndWorkspaceId(invoiceId, workspace.getId())
                .orElseThrow(() -> notFound("Invoice not found."));

        return BillingMapper.toInvoiceResponse(invoice, planName(invoice));
    }

    @Override
    public List<PaymentMethodResponse> listPaymentMethods(long userId) {
        Workspace workspace = getWorkspace(userId);

        return paymentMethodRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(BillingMapper::toPaymentMethodResponse)
                .toList();
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(long userId, CheckoutRequest request) {
        Workspace workspace = getWorkspace(userId);
        Plan plan = resolvePlan(request.getPlanId());
        BillingCycle cycle = parseCycle(request.getCycle());
        BigDecimal amount = priceFor(plan, cycle);

        if (amount.signum() <= 0) {
            activateSubscription(workspace, plan, cycle, LocalDateTime.now());
            return new CheckoutSessionResponse(
                    null,
                    null,
                    amount,
                    plan.getCurrency(),
                    plan.getName(),
                    BillingMapper.toCycleLabel(cycle)
            );
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentSession session = PaymentSession.builder()
                .workspace(workspace)
                .plan(plan)
                .billingCycle(cycle)
                .amount(amount)
                .currency(plan.getCurrency())
                .status(PaymentSessionStatus.PENDING)
                .expiresAt(now.plusMinutes(SESSION_TTL_MINUTES))
                .build();

        PaymentSession savedSession = paymentSessionRepository.save(session);
        String checkoutUrl = frontendUrl() + "/billing/checkout/" + savedSession.getId();

        return BillingMapper.toCheckoutSessionResponse(savedSession, checkoutUrl);
    }

    @Override
    public CheckoutSessionDetailsResponse getCheckoutSession(long userId, UUID sessionId) {
        Workspace workspace = getWorkspace(userId);
        PaymentSession session = paymentSessionRepository.findByIdAndWorkspaceId(sessionId, workspace.getId())
                .orElseThrow(() -> notFound("Payment session not found."));

        return BillingMapper.toCheckoutSessionDetails(session);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public CheckoutConfirmResponse confirmCheckoutSession(
            long userId,
            UUID sessionId,
            ConfirmCheckoutRequest request
    ) {
        Workspace workspace = getWorkspace(userId);
        PaymentSession session = paymentSessionRepository.findByIdAndWorkspaceId(sessionId, workspace.getId())
                .orElseThrow(() -> notFound("Payment session not found."));

        if (session.getStatus() == PaymentSessionStatus.PAID) {
            return existingConfirmation(session);
        }

        LocalDateTime now = LocalDateTime.now();

        if (session.getStatus() == PaymentSessionStatus.EXPIRED || isSessionExpired(session, now)) {
            if (session.getStatus() != PaymentSessionStatus.EXPIRED) {
                session.setStatus(PaymentSessionStatus.EXPIRED);
                paymentSessionRepository.save(session);
            }
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Payment session has expired.");
        }

        if (session.getStatus() != PaymentSessionStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment session is no longer pending.");
        }

        int expMonth = request.getExpMonth() == null ? 0 : request.getExpMonth();
        int expYear = request.getExpYear() == null ? 0 : request.getExpYear();
        PaymentResult result = paymentProvider.charge(new PaymentRequest(
                request.getCardNumber(),
                expMonth,
                expYear,
                request.getCvc(),
                request.getCardholderName()
        ));

        if (!result.successful()) {
            session.setStatus(PaymentSessionStatus.FAILED);
            session.setFailureReason(truncate(result.failureReason(), FAILURE_REASON_MAX_LENGTH));
            paymentSessionRepository.save(session);
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, result.failureReason());
        }

        session.setStatus(PaymentSessionStatus.PAID);
        session.setFailureReason(null);
        paymentSessionRepository.save(session);

        Subscription subscription = activateSubscription(workspace, session.getPlan(), session.getBillingCycle(), now);

        Payment payment = Payment.builder()
                .workspace(workspace)
                .paymentSession(session)
                .subscription(subscription)
                .amount(session.getAmount())
                .currency(session.getCurrency())
                .status(PaymentStatus.SUCCEEDED)
                .provider(paymentProvider.providerName())
                .providerReference(result.reference())
                .paidAt(now)
                .build();
        paymentRepository.save(payment);

        Invoice invoice = Invoice.builder()
                .workspace(workspace)
                .payment(payment)
                .invoiceNumber(nextInvoiceNumber())
                .amount(session.getAmount())
                .currency(session.getCurrency())
                .status(InvoiceStatus.PAID)
                .issuedAt(now)
                .paidAt(now)
                .build();
        invoiceRepository.save(invoice);

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .workspace(workspace)
                .brand(result.brand() == null ? "Card" : result.brand())
                .last4(result.last4())
                .expMonth(expMonth)
                .expYear(expYear)
                .isDefault(paymentMethodRepository.countByWorkspaceId(workspace.getId()) == 0)
                .build();
        paymentMethodRepository.save(paymentMethod);

        return BillingMapper.toCheckoutConfirmResponse(session, invoice, subscription.getCurrentPeriodEnd());
    }

    @Override
    @Transactional
    public BillingOverviewResponse updateSubscription(long userId, CancelSubscriptionRequest request) {
        Subscription subscription = getCurrentSubscription(userId);
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(request.getCancelAtPeriodEnd()));
        subscriptionRepository.save(subscription);

        return getOverview(userId);
    }

    @Override
    @Transactional
    public BillingOverviewResponse resumeSubscription(long userId) {
        Subscription subscription = getCurrentSubscription(userId);
        subscription.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(subscription);

        return getOverview(userId);
    }

    @Override
    @Transactional
    public PaymentMethodResponse setDefaultPaymentMethod(long userId, UUID paymentMethodId) {
        Workspace workspace = getWorkspace(userId);
        PaymentMethod target = paymentMethodRepository.findByIdAndWorkspaceId(paymentMethodId, workspace.getId())
                .orElseThrow(() -> notFound("Payment method not found."));

        paymentMethodRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .filter(paymentMethod -> paymentMethod.isDefault()
                        && !paymentMethod.getId().equals(target.getId()))
                .forEach(paymentMethod -> {
                    paymentMethod.setDefault(false);
                    paymentMethodRepository.save(paymentMethod);
                });

        target.setDefault(true);
        return BillingMapper.toPaymentMethodResponse(paymentMethodRepository.save(target));
    }

    @Override
    @Transactional
    public void deletePaymentMethod(long userId, UUID paymentMethodId) {
        Workspace workspace = getWorkspace(userId);
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByIdAndWorkspaceId(paymentMethodId, workspace.getId())
                .orElseThrow(() -> notFound("Payment method not found."));

        paymentMethodRepository.delete(paymentMethod);
    }

    private BillingOverviewResponse freeOverview(Plan starterPlan, long sitesUsed) {
        return BillingMapper.toOverview(
                starterPlan,
                "free",
                BillingCycle.MONTHLY,
                priceFor(starterPlan, BillingCycle.MONTHLY),
                null,
                false,
                null,
                sitesUsed
        );
    }

    private Subscription getCurrentSubscription(long userId) {
        Workspace workspace = getWorkspace(userId);
        LocalDateTime now = LocalDateTime.now();

        return subscriptionRepository.findByWorkspaceId(workspace.getId())
                .filter(subscription -> !isExpired(subscription, now))
                .orElseThrow(() -> notFound("Active subscription not found."));
    }

    private Subscription activateSubscription(
            Workspace workspace,
            Plan plan,
            BillingCycle cycle,
            LocalDateTime start
    ) {
        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspace.getId())
                .orElseGet(() -> Subscription.builder().workspace(workspace).build());

        LocalDateTime periodEnd = periodEnd(start, cycle);

        subscription.setPlan(plan);
        subscription.setStatus(ACTIVE_STATUS);
        subscription.setBillingCycle(cycle);
        subscription.setStartDate(start);
        subscription.setEndDate(periodEnd);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setCancelAtPeriodEnd(false);

        return subscriptionRepository.save(subscription);
    }

    private CheckoutConfirmResponse existingConfirmation(PaymentSession session) {
        Payment payment = paymentRepository
                .findFirstByPaymentSessionIdOrderByCreatedAtDesc(session.getId())
                .orElse(null);

        Invoice invoice = payment == null
                ? null
                : invoiceRepository.findFirstByPaymentIdOrderByCreatedAtDesc(payment.getId()).orElse(null);

        LocalDateTime currentPeriodEnd = subscriptionRepository.findByWorkspaceId(session.getWorkspace().getId())
                .map(Subscription::getCurrentPeriodEnd)
                .orElse(null);

        return BillingMapper.toCheckoutConfirmResponse(session, invoice, currentPeriodEnd);
    }

    private Plan resolvePlan(String planId) {
        String value = planId == null ? "" : planId.trim();

        Optional<Plan> plan;
        if (value.matches("\\d+")) {
            try {
                plan = planRepository.findById(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                plan = Optional.empty();
            }
        } else {
            plan = planRepository.findByCodeIgnoreCase(value);
        }

        return plan.filter(Plan::isActive)
                .orElseThrow(() -> notFound("Plan not found."));
    }

    private Workspace getWorkspace(long userId) {
        return workspaceRepository.findFirstByOwnerIdOrderByIdAsc(userId)
                .orElseThrow(() -> notFound("Workspace not found."));
    }

    private BillingCycle parseCycle(String cycle) {
        if (cycle == null || cycle.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Billing cycle is required.");
        }

        return switch (cycle.trim().toLowerCase(Locale.ROOT)) {
            case "monthly" -> BillingCycle.MONTHLY;
            case "annual", "yearly" -> BillingCycle.YEARLY;
            default -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid billing cycle.");
        };
    }

    private BigDecimal priceFor(Plan plan, BillingCycle cycle) {
        BigDecimal price = cycle == BillingCycle.YEARLY ? plan.getPriceYearly() : plan.getPriceMonthly();
        return price == null ? BigDecimal.ZERO : price;
    }

    private LocalDateTime periodEnd(LocalDateTime start, BillingCycle cycle) {
        return cycle == BillingCycle.YEARLY ? start.plusYears(1) : start.plusMonths(1);
    }

    private boolean isExpired(Subscription subscription, LocalDateTime now) {
        if ("EXPIRED".equalsIgnoreCase(subscription.getStatus())) {
            return true;
        }
        return subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isBefore(now);
    }

    private boolean isSessionExpired(PaymentSession session, LocalDateTime now) {
        return session.getExpiresAt() != null && session.getExpiresAt().isBefore(now);
    }

    private String overviewStatus(String status) {
        if (status == null) {
            return "active";
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "active";
            case "CANCELED", "CANCELLED" -> "canceled";
            case "PAST_DUE" -> "past_due";
            case "INCOMPLETE" -> "incomplete";
            case "EXPIRED" -> "free";
            default -> status.toLowerCase(Locale.ROOT);
        };
    }

    private String planName(Invoice invoice) {
        Payment payment = invoice.getPayment();
        if (payment == null) {
            return null;
        }

        if (payment.getPaymentSession() != null && payment.getPaymentSession().getPlan() != null) {
            return payment.getPaymentSession().getPlan().getName();
        }

        if (payment.getSubscription() != null && payment.getSubscription().getPlan() != null) {
            return payment.getSubscription().getPlan().getName();
        }

        return null;
    }

    private String nextInvoiceNumber() {
        String prefix = "INV-" + Year.now().getValue() + "-";
        long sequence = invoiceRepository.countByInvoiceNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%06d", sequence);

        while (invoiceRepository.existsByInvoiceNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%06d", sequence);
        }

        return candidate;
    }

    private String frontendUrl() {
        String url = appProperties.getFrontendUrl();
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
