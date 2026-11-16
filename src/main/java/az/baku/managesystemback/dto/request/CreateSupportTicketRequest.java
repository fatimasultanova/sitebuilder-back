package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
        @NotBlank(message = "Subject is required.")
        @Size(min = 4, max = 120, message = "Subject must be between 4 and 120 characters.")
        String subject,

        @NotBlank(message = "Category is required.")
        @Pattern(
                regexp = "account|website|billing|technical|other",
                message = "Choose a supported support category."
        )
        String category,

        @NotBlank(message = "Message is required.")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters.")
        String message
) {
}
