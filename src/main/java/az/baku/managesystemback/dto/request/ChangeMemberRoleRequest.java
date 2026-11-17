package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeMemberRoleRequest(

        @NotBlank
        @Pattern(
                regexp = "(?i)admin|editor|viewer",
                message = "Role must be admin, editor or viewer."
        )
        String role
) {
}
