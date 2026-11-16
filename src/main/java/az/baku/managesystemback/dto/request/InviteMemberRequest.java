package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteMemberRequest(@Email @NotBlank String email,
                                  @Pattern(regexp = "ADMIN|EDITOR|MEMBER|VIEWER") String role) {
}
