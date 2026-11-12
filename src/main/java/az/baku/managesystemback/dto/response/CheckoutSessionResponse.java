package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;

/**
 * @author Fatima Sultanova
 */
public record CheckoutSessionResponse(
        String sessionId,
        String checkoutUrl,
        BigDecimal amount,
        String currency,
        String planName,
        String cycle
) {
}
