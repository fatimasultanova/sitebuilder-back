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
public class DeploymentResponse {

    private final String id;
    private final String siteId;
    private final Integer version;
    private final String status;
    private final String publishedUrl;
    private final String logMessage;
    private final String triggeredBy;
    private final String createdAt;
    private final String startedAt;
    private final String finishedAt;
}
