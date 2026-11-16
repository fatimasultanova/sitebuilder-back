package az.baku.managesystemback.dto.request;

import jakarta.validation.constraints.Size;

public record GoogleOAuthStartRequest(String intent, @Size(max = 300) String returnTo) {
}
