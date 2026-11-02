package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.response.TemplateResponse;
import az.baku.managesystemback.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listTemplates() {
        return ResponseEntity.ok(templateService.listAvailableTemplates());
    }
}
