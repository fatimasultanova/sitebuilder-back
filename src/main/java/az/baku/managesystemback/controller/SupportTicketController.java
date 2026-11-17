package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.CreateSupportTicketRequest;
import az.baku.managesystemback.dto.response.SupportTicketResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.SupportTicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import az.baku.managesystemback.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping({"/api/support/requests", "/support/requests"})
@RequiredArgsConstructor
@Validated
public class SupportTicketController {
    private final SupportTicketService s;

    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody CreateSupportTicketRequest r) {
        return ResponseEntity.status(201).body(s.create(u.id(), r));
    }

    @GetMapping
    @Operation(summary = "List current user's support tickets")
    public PageResponse<SupportTicketResponse> list(
            @AuthenticationPrincipal AuthenticatedUser u,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return PageResponse.from(s.list(u.id(), page, size));
    }

    @GetMapping("/{id}")
    public SupportTicketResponse get(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id) {
        return s.get(u.id(), id);
    }
}
