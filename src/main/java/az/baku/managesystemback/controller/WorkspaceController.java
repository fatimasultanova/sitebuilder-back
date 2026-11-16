package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.*;
import az.baku.managesystemback.dto.response.*;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/workspaces", "/workspaces"})
@RequiredArgsConstructor
@Validated
public class WorkspaceController {
    private final WorkspaceService service;

    @GetMapping
    @Operation(summary = "List my workspaces")
    public Page<WorkspaceResponse> list(@AuthenticationPrincipal AuthenticatedUser u, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(u.id(), page, size);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse get(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id) {
        return service.get(u.id(), id);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody WorkspaceRequest r) {
        return ResponseEntity.status(201).body(service.create(u.id(), r));
    }

    @PutMapping("/{id}")
    public WorkspaceResponse update(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @Valid @RequestBody WorkspaceRequest r) {
        return service.update(u.id(), id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id) {
        service.delete(u.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal AuthenticatedUser u, @RequestBody java.util.Map<String, String> body) {
        service.accept(u.id(), body.getOrDefault("token", ""));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public Page<MemberResponse> members(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listMembers(u.id(), id, page, size);
    }

    @GetMapping("/{id}/members/{memberId}")
    @Operation(summary = "Get workspace member")
    public MemberResponse getMember(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long id,
            @PathVariable long memberId
    ) {
        return service.getMember(user.id(), id, memberId);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<String> invite(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @Valid @RequestBody InviteMemberRequest r) {
        return ResponseEntity.status(201).body(service.invite(u.id(), id, r));
    }

    @PostMapping("/{id}/members/{memberId}/invite/resend")
    public ResponseEntity<Void> resend(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @PathVariable long memberId) {
        service.resend(u.id(), id, memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{memberId}")
    public MemberResponse role(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @PathVariable long memberId, @Valid @RequestBody ChangeMemberRoleRequest r) {
        return service.role(u.id(), id, memberId, r);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser u, @PathVariable long id, @PathVariable long memberId) {
        service.remove(u.id(), id, memberId);
        return ResponseEntity.noContent().build();
    }
}
