package az.baku.managesystemback.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AccountSessionResponse(
        String id,
        String browser,
        String os,
        String device,
        String location,
        String ipAddress,
        Instant lastActiveAt,
        Instant createdAt,
        @JsonProperty("isCurrent") boolean isCurrent
) {
}
