package az.baku.managesystemback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Fatima Sultanova
 */
@Getter
@Builder
@AllArgsConstructor
public class SiteDomainResponse {

    private final String hostname;
    private final String status;
    private final String ssl;
    private final List<DnsRecordResponse> records;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime lastCheckedAt;
    private final String error;
}
