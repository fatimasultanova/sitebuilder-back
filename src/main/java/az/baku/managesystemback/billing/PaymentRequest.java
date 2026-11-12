package az.baku.managesystemback.billing;

/**
 * Card data is intentionally excluded from toString so it can never leak into logs.
 *
 * @author Fatima Sultanova
 */
public record PaymentRequest(
        String cardNumber,
        int expMonth,
        int expYear,
        String cvc,
        String cardholderName
) {

    @Override
    public String toString() {
        return "PaymentRequest[expMonth=" + expMonth
                + ", expYear=" + expYear
                + ", cardholderName=" + cardholderName + "]";
    }
}
