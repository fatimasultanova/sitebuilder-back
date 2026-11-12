package az.baku.managesystemback.service;

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

import java.util.List;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
public interface BillingService {

    List<PlanResponse> listPlans();

    BillingOverviewResponse getOverview(long userId);

    List<InvoiceResponse> listInvoices(long userId);

    InvoiceResponse getInvoiceReceipt(long userId, UUID invoiceId);

    List<PaymentMethodResponse> listPaymentMethods(long userId);

    CheckoutSessionResponse createCheckoutSession(long userId, CheckoutRequest request);

    CheckoutSessionDetailsResponse getCheckoutSession(long userId, UUID sessionId);

    CheckoutConfirmResponse confirmCheckoutSession(long userId, UUID sessionId, ConfirmCheckoutRequest request);

    BillingOverviewResponse updateSubscription(long userId, CancelSubscriptionRequest request);

    BillingOverviewResponse resumeSubscription(long userId);

    PaymentMethodResponse setDefaultPaymentMethod(long userId, UUID paymentMethodId);

    void deletePaymentMethod(long userId, UUID paymentMethodId);
}
