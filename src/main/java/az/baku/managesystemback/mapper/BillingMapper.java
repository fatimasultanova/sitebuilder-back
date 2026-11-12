package az.baku.managesystemback.mapper;

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
import az.baku.managesystemback.entity.PaymentMethod;
import az.baku.managesystemback.entity.PaymentSession;
import az.baku.managesystemback.entity.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Fatima Sultanova
 */
public final class BillingMapper {

    private BillingMapper() {
    }

    public static PlanResponse toPlanResponse(Plan plan) {
        BigDecimal monthly = plan.getPriceMonthly() == null ? BigDecimal.ZERO : plan.getPriceMonthly();
        BigDecimal yearly = plan.getPriceYearly() == null ? BigDecimal.ZERO : plan.getPriceYearly();

        PlanResponse.Prices prices = new PlanResponse.Prices(
                new PlanResponse.Price(monthly, plan.getCurrency()),
                new PlanResponse.Price(yearly, plan.getCurrency())
        );

        return new PlanResponse(
                String.valueOf(plan.getId()),
                plan.getCode(),
                plan.getName(),
                planDescription(plan),
                monthly,
                plan.getCurrency(),
                plan.getRank(),
                true,
                isRecommended(plan),
                prices,
                planFeatures(plan)
        );
    }

    public static BillingOverviewResponse toOverview(
            Plan plan,
            String status,
            BillingCycle cycle,
            BigDecimal price,
            LocalDateTime renewsAt,
            boolean cancelAtPeriodEnd,
            String paymentMethodId,
            long sitesUsed
    ) {
        return new BillingOverviewResponse(
                String.valueOf(plan.getId()),
                plan.getName(),
                status,
                toCycleLabel(cycle),
                price,
                plan.getCurrency(),
                renewsAt,
                cancelAtPeriodEnd,
                paymentMethodId,
                plan.getMaxSites(),
                sitesUsed
        );
    }

    public static PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                String.valueOf(paymentMethod.getId()),
                "card",
                paymentMethod.getBrand(),
                paymentMethod.getLast4(),
                expiry(paymentMethod.getExpMonth(), paymentMethod.getExpYear()),
                paymentMethod.isDefault()
        );
    }

    public static InvoiceResponse toInvoiceResponse(Invoice invoice, String planName) {
        return new InvoiceResponse(
                String.valueOf(invoice.getId()),
                invoice.getInvoiceNumber(),
                invoice.getIssuedAt(),
                invoice.getAmount(),
                invoice.getCurrency(),
                toInvoiceStatus(invoice.getStatus()),
                planName,
                "/billing/invoices/" + invoice.getId() + "/receipt"
        );
    }

    public static CheckoutSessionResponse toCheckoutSessionResponse(
            PaymentSession session,
            String checkoutUrl
    ) {
        return new CheckoutSessionResponse(
                session.getId() == null ? null : String.valueOf(session.getId()),
                checkoutUrl,
                session.getAmount(),
                session.getCurrency(),
                session.getPlan().getName(),
                toCycleLabel(session.getBillingCycle())
        );
    }

    public static CheckoutSessionDetailsResponse toCheckoutSessionDetails(PaymentSession session) {
        return new CheckoutSessionDetailsResponse(
                String.valueOf(session.getId()),
                String.valueOf(session.getPlan().getId()),
                session.getPlan().getName(),
                toCycleLabel(session.getBillingCycle()),
                session.getAmount(),
                session.getCurrency(),
                session.getStatus().name().toLowerCase(Locale.ROOT),
                session.getExpiresAt()
        );
    }

    public static CheckoutConfirmResponse toCheckoutConfirmResponse(
            PaymentSession session,
            Invoice invoice,
            LocalDateTime currentPeriodEnd
    ) {
        return new CheckoutConfirmResponse(
                String.valueOf(session.getId()),
                session.getStatus().name().toLowerCase(Locale.ROOT),
                session.getAmount(),
                session.getCurrency(),
                session.getPlan().getName(),
                toCycleLabel(session.getBillingCycle()),
                invoice == null ? null : String.valueOf(invoice.getId()),
                invoice == null ? null : invoice.getInvoiceNumber(),
                currentPeriodEnd
        );
    }

    public static String toCycleLabel(BillingCycle cycle) {
        return cycle == BillingCycle.YEARLY ? "annual" : "monthly";
    }

    private static List<PlanResponse.Feature> planFeatures(Plan plan) {
        List<PlanResponse.Feature> features = new ArrayList<>();
        features.add(new PlanResponse.Feature(websitesLabel(plan.getMaxSites()), true));
        features.add(new PlanResponse.Feature("Custom domain", plan.isCustomDomainAllowed()));
        features.add(new PlanResponse.Feature("Remove branding", plan.isRemoveBranding()));
        features.add(new PlanResponse.Feature("Team access", plan.isTeamAllowed()));
        return List.copyOf(features);
    }

    private static String websitesLabel(int maxSites) {
        if (maxSites < 0) {
            return "Unlimited websites";
        }
        return maxSites == 1 ? "1 website" : maxSites + " websites";
    }

    private static boolean isRecommended(Plan plan) {
        return "PRO".equalsIgnoreCase(plan.getCode());
    }

    private static String planDescription(Plan plan) {
        Object description = plan.getFeatures() == null ? null : plan.getFeatures().get("description");
        if (description instanceof String text && !text.isBlank()) {
            return text;
        }

        return switch (plan.getCode() == null ? "" : plan.getCode().toUpperCase(Locale.ROOT)) {
            case "STARTER" -> "Get started with a single website.";
            case "PRO" -> "Everything you need to grow your online presence.";
            case "AGENCY" -> "Unlimited websites for teams and agencies.";
            default -> plan.getName() + " plan.";
        };
    }

    private static String toInvoiceStatus(InvoiceStatus status) {
        if (status == null) {
            return "open";
        }

        return switch (status) {
            case PAID -> "paid";
            case ISSUED -> "open";
            case VOID -> "void";
            case REFUNDED -> "refunded";
        };
    }

    private static String expiry(int expMonth, int expYear) {
        return String.format("%02d/%02d", expMonth, Math.floorMod(expYear, 100));
    }
}
