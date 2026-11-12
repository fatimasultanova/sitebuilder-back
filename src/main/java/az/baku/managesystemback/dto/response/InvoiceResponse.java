package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
public record InvoiceResponse(
        String id,
        String number,
        LocalDateTime issuedAt,
        BigDecimal amount,
        String currency,
        String status,
        String planName,
        String receiptUrl
) {
}
