package az.baku.managesystemback.mapper;

import az.baku.managesystemback.dto.response.DnsRecordResponse;
import az.baku.managesystemback.dto.response.SiteDomainResponse;
import az.baku.managesystemback.entity.SiteDomain;

import java.util.List;
import java.util.Locale;

/**
 * @author Fatima Sultanova
 */
public final class SiteDomainMapper {

    private static final String UNCONFIGURED = "unconfigured";

    private SiteDomainMapper() {
    }

    public static SiteDomainResponse toResponse(SiteDomain domain, List<DnsRecordResponse> records) {
        return SiteDomainResponse.builder()
                .hostname(domain.getHostname())
                .status(toStatusValue(domain.getStatus()))
                .ssl(toStatusValue(domain.getSslStatus()))
                .records(records)
                .verifiedAt(domain.getVerifiedAt())
                .lastCheckedAt(domain.getLastCheckedAt())
                .error(domain.getErrorMessage())
                .build();
    }

    public static SiteDomainResponse unconfigured() {
        return SiteDomainResponse.builder()
                .hostname(null)
                .status(UNCONFIGURED)
                .ssl(UNCONFIGURED)
                .records(List.of())
                .verifiedAt(null)
                .lastCheckedAt(null)
                .error(null)
                .build();
    }

    public static String toStatusValue(Enum<?> status) {
        return status == null ? null : status.name().toLowerCase(Locale.ROOT);
    }
}
