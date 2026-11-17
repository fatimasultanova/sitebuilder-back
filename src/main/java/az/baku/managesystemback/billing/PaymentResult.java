package az.baku.managesystemback.billing;

/**
 * @author Fatima Sultanova
 */
public record PaymentResult(
        boolean successful,
        String reference,
        String failureReason,
        String brand,
        String last4
) {

    public static PaymentResult success(String reference, String brand, String last4) {
        return new PaymentResult(true, reference, null, brand, last4);
    }

    public static PaymentResult failure(String failureReason) {
        return new PaymentResult(false, null, failureReason, null, null);
    }
}
