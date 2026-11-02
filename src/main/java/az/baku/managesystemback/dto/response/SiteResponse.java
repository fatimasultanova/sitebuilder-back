package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.SiteStatus;
import com.fasterxml.jackson.databind.JsonNode;
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
public class SiteResponse {

    private final Long id;
    private final Long templateId;
    private final String templateName;
    private final String projectName;
    private final String slug;
    private final SiteStatus status;
    private final JsonNode content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
