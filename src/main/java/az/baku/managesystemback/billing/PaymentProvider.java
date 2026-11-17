package az.baku.managesystemback.billing;

/**
 * @author Fatima Sultanova
 */
public interface PaymentProvider {

    String providerName();

    PaymentResult charge(PaymentRequest request);
}
