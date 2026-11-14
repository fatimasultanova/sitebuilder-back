package az.baku.managesystemback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * @author Fatima Sultanova
 */
@Getter
@Builder
@AllArgsConstructor
public class PublicSiteResponse {

    private final String siteId;
    private final String slug;
    private final String projectName;
    private final Integer version;
    private final String publishedUrl;
    private final String publishedAt;
    private final Map<String, Object> snapshot;
}
