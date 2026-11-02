package az.baku.managesystemback.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Fatima Sultanova
 */
@Getter
@Builder
@AllArgsConstructor
public class TemplateResponse {

    private final Long id;
    private final String name;
    private final String category;
    private final String description;
    private final String previewImageUrl;
    private final boolean premium;
    private final JsonNode themeConfig;
}
