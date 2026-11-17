package az.baku.managesystemback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Fatima Sultanova
 */
@Getter
@Builder
@AllArgsConstructor
public class DnsRecordResponse {

    private final String type;
    private final String name;
    private final String value;
}
