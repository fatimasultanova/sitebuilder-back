package az.baku.managesystemback.controller;

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
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping({"/billing", "/api/billing"})
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Plans, subscriptions, checkout, invoices and payment methods")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    @Operation(summary = "List active billing plans")
    public ResponseEntity<List<PlanResponse>> listPlans() {
        return ResponseEntity.ok(billingService.listPlans());
    }

    @GetMapping
    @Operation(summary = "Get the current workspace billing overview")
    public ResponseEntity<BillingOverviewResponse> getOverview(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.getOverview(principal.id()));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List the current workspace invoices")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.listInvoices(principal.id()));
    }

    @GetMapping("/invoices/{id}/receipt")
    @Operation(summary = "Get an invoice receipt as JSON")
    public ResponseEntity<InvoiceResponse> getInvoiceReceipt(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(billingService.getInvoiceReceipt(principal.id(), id));
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "List the current workspace payment methods")
    public ResponseEntity<List<PaymentMethodResponse>> listPaymentMethods(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.listPaymentMethods(principal.id()));
    }

    @PostMapping("/checkout-session")
    @Operation(summary = "Create a checkout session for a plan and billing cycle")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return ResponseEntity.ok(billingService.createCheckoutSession(principal.id(), request));
    }

    @GetMapping("/checkout-session/{id}")
    @Operation(summary = "Get a checkout session owned by the current workspace")
    public ResponseEntity<CheckoutSessionDetailsResponse> getCheckoutSession(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(billingService.getCheckoutSession(principal.id(), id));
    }

    @PostMapping("/checkout-session/{id}/confirm")
    @Operation(summary = "Confirm a checkout session with card details")
    public ResponseEntity<CheckoutConfirmResponse> confirmCheckoutSession(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmCheckoutRequest request
    ) {
        return ResponseEntity.ok(billingService.confirmCheckoutSession(principal.id(), id, request));
    }

    @PostMapping("/subscription")
    @Operation(summary = "Cancel the current subscription at the end of the billing period")
    public ResponseEntity<BillingOverviewResponse> updateSubscription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CancelSubscriptionRequest request
    ) {
        return ResponseEntity.ok(billingService.updateSubscription(principal.id(), request));
    }

    @PostMapping("/subscription/resume")
    @Operation(summary = "Resume the current subscription")
    public ResponseEntity<BillingOverviewResponse> resumeSubscription(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(billingService.resumeSubscription(principal.id()));
    }

    @PostMapping("/payment-methods/{id}/default")
    @Operation(summary = "Set a payment method as the workspace default")
    public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(billingService.setDefaultPaymentMethod(principal.id(), id));
    }

    @DeleteMapping("/payment-methods/{id}")
    @Operation(summary = "Delete a payment method")
    public ResponseEntity<Void> deletePaymentMethod(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        billingService.deletePaymentMethod(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
