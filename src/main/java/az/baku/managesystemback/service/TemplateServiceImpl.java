package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.response.TemplateResponse;
import az.baku.managesystemback.mapper.TemplateMapper;
import az.baku.managesystemback.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    public List<TemplateResponse> listAvailableTemplates() {
        return templateRepository.findPublishedTemplates()
                .stream()
                .map(TemplateMapper::toResponse)
                .toList();
    }
}
