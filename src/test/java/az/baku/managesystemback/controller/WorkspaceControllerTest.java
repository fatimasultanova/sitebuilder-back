package az.baku.managesystemback.controller;

import az.baku.managesystemback.dto.request.ChangeMemberRoleRequest;
import az.baku.managesystemback.dto.request.InviteMemberRequest;
import az.baku.managesystemback.dto.response.MemberResponse;
import az.baku.managesystemback.security.AuthenticatedUser;
import az.baku.managesystemback.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkspaceControllerTest {

    private WorkspaceService service;
    private WorkspaceController controller;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        service = mock(WorkspaceService.class);
        controller = new WorkspaceController(service);

        principal = new AuthenticatedUser(
                1L,
                "owner@example.com",
                "ROLE_USER",
                UUID.randomUUID()
        );
    }

    @Test
    void inviteShouldReturnCreatedAndMemberResponse() {
        InviteMemberRequest request =
                new InviteMemberRequest(
                        "invitee@example.com",
                        "viewer"
                );

        MemberResponse memberResponse =
                new MemberResponse(
                        "10",
                        "20",
                        "invitee@example.com",
                        "Invitee",
                        "viewer",
                        "invited",
                        Instant.now(),
                        null
                );

        when(service.invite(1L, 5L, request))
                .thenReturn(memberResponse);

        ResponseEntity<MemberResponse> response =
                controller.invite(
                        principal,
                        5L,
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .isEqualTo(memberResponse);

        verify(service).invite(
                1L,
                5L,
                request
        );
    }

    @Test
    void roleShouldReturnUpdatedMemberResponse() {
        ChangeMemberRoleRequest request =
                new ChangeMemberRoleRequest("admin");

        MemberResponse memberResponse =
                new MemberResponse(
                        "10",
                        "20",
                        "invitee@example.com",
                        "Invitee",
                        "admin",
                        "active",
                        Instant.now(),
                        Instant.now()
                );

        when(service.role(
                1L,
                5L,
                10L,
                request
        )).thenReturn(memberResponse);

        MemberResponse response =
                controller.role(
                        principal,
                        5L,
                        10L,
                        request
                );

        assertThat(response.role())
                .isEqualTo("admin");

        verify(service).role(
                1L,
                5L,
                10L,
                request
        );
    }

    @Test
    void removeShouldReturnNoContent() {
        ResponseEntity<Void> response =
                controller.remove(
                        principal,
                        5L,
                        10L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).remove(
                1L,
                5L,
                10L
        );
    }

    @Test
    void deleteWorkspaceShouldReturnNoContent() {
        ResponseEntity<Void> response =
                controller.delete(
                        principal,
                        5L
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).delete(
                1L,
                5L
        );
    }
}
