package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
public record CheckoutConfirmResponse(
        String sessionId,
        String status,
        BigDecimal amount,
        String currency,
        String planName,
        String cycle,
        String invoiceId,
        String invoiceNumber,
        LocalDateTime currentPeriodEnd
) {
}
