package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.UpdateProfileRequest;
import az.baku.managesystemback.dto.response.ProfileResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/profile", "/profile"})
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping
    @Operation(summary = "Get current profile")
    public ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser u) {
        return service.get(u.id());
    }

    @PatchMapping
    @Operation(summary = "Update current profile")
    public ProfileResponse update(@AuthenticationPrincipal AuthenticatedUser u, @Valid @RequestBody UpdateProfileRequest r) {
        return service.update(u.id(), r);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse avatar(@AuthenticationPrincipal AuthenticatedUser u, @RequestPart("avatar") MultipartFile f) {
        return service.avatar(u.id(), f);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser u) {
        service.deleteAvatar(u.id());
        return ResponseEntity.noContent().build();
    }
}

