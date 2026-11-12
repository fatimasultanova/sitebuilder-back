package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
public record CheckoutSessionDetailsResponse(
        String sessionId,
        String planId,
        String planName,
        String cycle,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime expiresAt
) {
}
