package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
public record BillingOverviewResponse(
        String planId,
        String planName,
        String status,
        String cycle,
        BigDecimal price,
        String currency,
        LocalDateTime renewsAt,
        boolean cancelAtPeriodEnd,
        String paymentMethodId,
        int maxSites,
        long sitesUsed
) {
}
