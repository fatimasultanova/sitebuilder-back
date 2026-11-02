package az.baku.managesystemback.mapper;

import az.baku.managesystemback.dto.response.TemplateResponse;
import az.baku.managesystemback.entity.Template;

/**
 * @author Fatima Sultanova
 */
public final class TemplateMapper {

    private TemplateMapper() {
    }

    public static TemplateResponse toResponse(Template template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .category(template.getCategory())
                .description(template.getDescription())
                .previewImageUrl(template.getPreviewImageUrl())
                .premium(template.isPremium())
                .themeConfig(template.getThemeConfig())
                .build();
    }
}
