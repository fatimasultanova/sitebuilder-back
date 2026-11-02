package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.response.TemplateResponse;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
public interface TemplateService {

    List<TemplateResponse> listAvailableTemplates();
}
