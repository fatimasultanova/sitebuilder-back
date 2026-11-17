package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Fatima Sultanova
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelSubscriptionRequest {

    @NotNull(message = "cancelAtPeriodEnd is required.")
    private Boolean cancelAtPeriodEnd;
}
