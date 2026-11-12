package az.baku.managesystemback.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Fatima Sultanova
 */
public record PlanResponse(
        String id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int rank,
        boolean purchasable,
        boolean recommended,
        Prices prices,
        List<Feature> features
) {

    public record Prices(Price monthly, Price annual) {
    }

    public record Price(BigDecimal amount, String currency) {
    }

    public record Feature(String label, boolean included) {
    }
}
