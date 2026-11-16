package az.baku.managesystemback.controller;

import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.ActivityService;
import az.baku.managesystemback.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class ActivityController {
    private final ActivityService service;

    @GetMapping({"/api/activity", "/activity"})
    public List<Map<String, Object>> activity(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be 1–200.");
        }
        return service.list(principal.id(), page, size);
    }

    @GetMapping({"/api/usage", "/usage"})
    public Map<String, Object> usage(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.usage(principal.id());
    }

    @GetMapping({"/api/usage/limits", "/usage/limits"})
    public Map<String, Object> limits(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.limits(principal.id());
    }
}
