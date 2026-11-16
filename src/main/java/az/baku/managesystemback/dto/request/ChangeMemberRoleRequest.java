package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.Pattern;

public record ChangeMemberRoleRequest(@Pattern(regexp = "ADMIN|EDITOR|MEMBER|VIEWER") String role) {
}
