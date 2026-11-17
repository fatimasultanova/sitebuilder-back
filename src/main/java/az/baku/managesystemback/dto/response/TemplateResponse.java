package az.baku.managesystemback.dto.response;

import java.util.Map;

/**
 * @author Fatima Sultanova
 */
public record TemplateResponse(
        Long id,
        String name,
        String category,
        String description,
        String previewImageUrl,
        boolean premium,
        Map<String, Object> themeConfig
) {
}
