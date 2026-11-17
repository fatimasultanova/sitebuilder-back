package az.baku.managesystemback.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Deterministic fake provider used for local development and tests.
 * Card numbers and CVC are used only for the in-memory check and are never logged or stored.
 *
 * @author Fatima Sultanova
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "provider", havingValue = "fake", matchIfMissing = true)
public class FakePaymentProvider implements PaymentProvider {

    private static final String INVALID_CARD = "Invalid card";
    private static final String CARD_DECLINED = "Card declined";
    private static final String INSUFFICIENT_FUNDS = "Insufficient funds";

    @Override
    public String providerName() {
        return "fake";
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        String cardNumber = normalizeCardNumber(request.cardNumber());

        if (!cardNumber.matches("\\d{12,19}") || !passesLuhnCheck(cardNumber)) {
            return PaymentResult.failure(INVALID_CARD);
        }

        YearMonth expiry = parseExpiry(request.expMonth(), request.expYear());
        if (expiry == null) {
            return PaymentResult.failure(INVALID_CARD);
        }
        if (expiry.isBefore(YearMonth.now())) {
            return PaymentResult.failure("Card expired");
        }

        return switch (cardNumber) {
            case "4242424242424242" -> PaymentResult.success(
                    "fake_" + UUID.randomUUID(),
                    detectBrand(cardNumber),
                    last4(cardNumber)
            );
            case "4000000000000002" -> PaymentResult.failure(CARD_DECLINED);
            case "4000000000009995" -> PaymentResult.failure(INSUFFICIENT_FUNDS);
            default -> PaymentResult.failure(INVALID_CARD);
        };
    }

    private String normalizeCardNumber(String cardNumber) {
        return cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
    }

    private YearMonth parseExpiry(int expMonth, int expYear) {
        if (expMonth < 1 || expMonth > 12) {
            return null;
        }

        int fullYear = expYear < 100 ? 2000 + expYear : expYear;
        if (fullYear < 2000 || fullYear > 2200) {
            return null;
        }

        return YearMonth.of(fullYear, expMonth);
    }

    private boolean passesLuhnCheck(String cardNumber) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    private String detectBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "Visa";
        }
        if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
            return "American Express";
        }
        if (cardNumber.startsWith("6011") || cardNumber.startsWith("65")) {
            return "Discover";
        }
        if (cardNumber.startsWith("5")) {
            return "Mastercard";
        }
        return "Card";
    }

    private String last4(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }
}
