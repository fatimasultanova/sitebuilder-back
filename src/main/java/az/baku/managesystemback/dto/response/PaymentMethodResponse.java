package az.baku.managesystemback.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Fatima Sultanova
 */
public record PaymentMethodResponse(
        String id,
        String type,
        String brand,
        String last4,
        String expiry,
        @JsonProperty("isDefault") boolean isDefault
) {
}
