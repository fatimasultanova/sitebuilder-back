package az.baku.managesystemback.dto.response;

import java.time.Instant;

public record SecurityActivityResponse(
        String id,
        String type,
        Instant occurredAt,
        String device,
        String location,
        String ipAddress,
        String detail
) {
}
