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
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getDescription(),
                template.getPreviewImageUrl(),
                template.isPremium(),
                template.getThemeConfig()
        );
    }
}
