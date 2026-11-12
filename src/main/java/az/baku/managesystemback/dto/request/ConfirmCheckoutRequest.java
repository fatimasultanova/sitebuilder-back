package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card data is used only for the payment attempt. It is never stored or logged.
 *
 * @author Fatima Sultanova
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmCheckoutRequest {

    @NotBlank(message = "Card number is required.")
    private String cardNumber;

    @NotNull(message = "Expiry month is required.")
    @Min(value = 1, message = "Expiry month must be between 1 and 12.")
    @Max(value = 12, message = "Expiry month must be between 1 and 12.")
    private Integer expMonth;

    @NotNull(message = "Expiry year is required.")
    @Min(value = 0, message = "Expiry year is invalid.")
    private Integer expYear;

    @NotBlank(message = "CVC is required.")
    private String cvc;

    @NotBlank(message = "Cardholder name is required.")
    private String cardholderName;
}
