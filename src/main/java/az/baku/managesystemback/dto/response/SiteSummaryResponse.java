package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.SiteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
@Getter
@Builder
@AllArgsConstructor
public class SiteSummaryResponse {

    private final Long id;
    private final Long templateId;
    private final String templateName;
    private final String projectName;
    private final String slug;
    private final SiteStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
