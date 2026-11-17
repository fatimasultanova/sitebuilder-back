package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.ChangeMemberRoleRequest;
import az.baku.managesystemback.dto.request.InviteMemberRequest;
import az.baku.managesystemback.dto.response.MemberResponse;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.entity.WorkspaceMember;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.repository.WorkspaceMemberRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import az.baku.managesystemback.event.WorkspaceInvitationRequested;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkspaceServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long WORKSPACE_ID = 10L;
    private static final long MEMBER_ID = 20L;
    private static final long INVITEE_ID = 30L;

    private WorkspaceRepository workspaces;
    private WorkspaceMemberRepository members;
    private UserRepository users;
    private ApplicationEventPublisher events;
    private NotificationService notificationService;

    private WorkspaceService service;

    private User owner;
    private User invitee;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspaces = mock(WorkspaceRepository.class);
        members = mock(WorkspaceMemberRepository.class);
        users = mock(UserRepository.class);
        events = mock(ApplicationEventPublisher.class);
        notificationService = mock(NotificationService.class);

        service = new WorkspaceService(
                workspaces,
                members,
                users,
                events,
                notificationService
        );

        owner = User.builder()
                .id(OWNER_ID)
                .name("Owner")
                .surname("User")
                .email("owner@example.com")
                .password("encoded")
                .build();

        invitee = User.builder()
                .id(INVITEE_ID)
                .name("Invitee")
                .surname("User")
                .email("invitee@example.com")
                .password("encoded")
                .build();

        workspace = Workspace.builder()
                .id(WORKSPACE_ID)
                .name("Test Workspace")
                .owner(owner)
                .build();
    }

    @Test
    void inviteShouldCreateInvitedViewerAndReturnMemberResponse() {
        InviteMemberRequest request =
                new InviteMemberRequest("invitee@example.com", "viewer");

        when(workspaces.findByIdAndOwnerId(WORKSPACE_ID, OWNER_ID))
                .thenReturn(Optional.of(workspace));

        when(users.findByEmailIgnoreCase("invitee@example.com"))
                .thenReturn(Optional.of(invitee));

        when(members.existsByWorkspaceIdAndUserId(
                WORKSPACE_ID,
                INVITEE_ID
        )).thenReturn(false);

        when(members.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> {
                    WorkspaceMember member = invocation.getArgument(0);
                    member.setId(MEMBER_ID);
                    return member;
                });

        MemberResponse response =
                service.invite(OWNER_ID, WORKSPACE_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(Long.toString(MEMBER_ID));
        assertThat(response.userId()).isEqualTo(Long.toString(INVITEE_ID));
        assertThat(response.role()).isEqualTo("viewer");
        assertThat(response.status()).isEqualTo("invited");
        assertThat(response.invitedAt()).isNotNull();
        assertThat(response.joinedAt()).isNull();

        verify(members).save(argThat(member ->
                "VIEWER".equals(member.getRole())
                        && "INVITED".equals(member.getStatus())
                        && member.getInvitationTokenHash() != null
                        && member.getInvitationExpiresAt() != null
        ));

        verify(events).publishEvent(
                any(WorkspaceInvitationRequested.class)
        );
        verify(notificationService).createForUser(
                eq(INVITEE_ID),
                eq("system"),
                eq("info"),
                eq("Workspace invitation"),
                anyString()
        );
    }

    @Test
    void inviteShouldNormalizeEditorRoleToUppercaseInDatabase() {
        InviteMemberRequest request =
                new InviteMemberRequest("invitee@example.com", "editor");

        when(workspaces.findByIdAndOwnerId(WORKSPACE_ID, OWNER_ID))
                .thenReturn(Optional.of(workspace));

        when(users.findByEmailIgnoreCase("invitee@example.com"))
                .thenReturn(Optional.of(invitee));

        when(members.existsByWorkspaceIdAndUserId(
                WORKSPACE_ID,
                INVITEE_ID
        )).thenReturn(false);

        when(members.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> {
                    WorkspaceMember member = invocation.getArgument(0);
                    member.setId(MEMBER_ID);
                    return member;
                });

        MemberResponse response =
                service.invite(OWNER_ID, WORKSPACE_ID, request);

        assertThat(response.role()).isEqualTo("editor");

        verify(members).save(argThat(member ->
                "EDITOR".equals(member.getRole())
        ));
    }

    @Test
    void listMembersShouldIncludeOwnerAndInvitedMember() {
        Instant invitedAt = Instant.now();

        WorkspaceMember invited = WorkspaceMember.builder()
                .id(MEMBER_ID)
                .workspace(workspace)
                .user(invitee)
                .role("VIEWER")
                .status("INVITED")
                .createdAt(invitedAt)
                .build();

        when(workspaces.findVisibleByIdAndUser(
                WORKSPACE_ID,
                OWNER_ID
        )).thenReturn(Optional.of(workspace));

        when(members.findAllByWorkspaceId(
                eq(WORKSPACE_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(invited)));

        var result = service.listMembers(
                OWNER_ID,
                WORKSPACE_ID,
                0,
                20
        );

        assertThat(result.getContent()).hasSize(2);

        MemberResponse ownerResponse = result.getContent().get(0);
        MemberResponse invitedResponse = result.getContent().get(1);

        assertThat(ownerResponse.id()).isEqualTo("owner");
        assertThat(ownerResponse.role()).isEqualTo("owner");
        assertThat(ownerResponse.status()).isEqualTo("active");

        assertThat(invitedResponse.id())
                .isEqualTo(Long.toString(MEMBER_ID));
        assertThat(invitedResponse.role()).isEqualTo("viewer");
        assertThat(invitedResponse.status()).isEqualTo("invited");
        assertThat(invitedResponse.invitedAt()).isEqualTo(invitedAt);
        assertThat(invitedResponse.joinedAt()).isNull();

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getMemberShouldReturnInvitedMember() {
        WorkspaceMember invited = WorkspaceMember.builder()
                .id(MEMBER_ID)
                .workspace(workspace)
                .user(invitee)
                .role("VIEWER")
                .status("INVITED")
                .build();

        when(workspaces.findVisibleByIdAndUser(
                WORKSPACE_ID,
                OWNER_ID
        )).thenReturn(Optional.of(workspace));

        when(members.findById(MEMBER_ID))
                .thenReturn(Optional.of(invited));

        MemberResponse response =
                service.getMember(
                        OWNER_ID,
                        WORKSPACE_ID,
                        MEMBER_ID
                );

        assertThat(response.status()).isEqualTo("invited");
        assertThat(response.role()).isEqualTo("viewer");
    }

    @Test
    void roleShouldNormalizeRoleBeforeSaving() {
        WorkspaceMember member = WorkspaceMember.builder()
                .id(MEMBER_ID)
                .workspace(workspace)
                .user(invitee)
                .role("VIEWER")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        when(workspaces.findByIdAndOwnerId(
                WORKSPACE_ID,
                OWNER_ID
        )).thenReturn(Optional.of(workspace));

        when(members.findById(MEMBER_ID))
                .thenReturn(Optional.of(member));

        when(members.save(member)).thenReturn(member);

        MemberResponse response = service.role(
                OWNER_ID,
                WORKSPACE_ID,
                MEMBER_ID,
                new ChangeMemberRoleRequest("admin")
        );

        assertThat(member.getRole()).isEqualTo("ADMIN");
        assertThat(response.role()).isEqualTo("admin");

        verify(members).save(member);
    }

    @Test
    void acceptShouldActivateInvitationAndSetJoinedAt() {
        String token = "test-token";

        WorkspaceMember invited = WorkspaceMember.builder()
                .id(MEMBER_ID)
                .workspace(workspace)
                .user(invitee)
                .role("VIEWER")
                .status("INVITED")
                .invitationTokenHash("stored-hash")
                .invitationExpiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(members.findByInvitationTokenHash(anyString()))
                .thenReturn(Optional.of(invited));

        service.accept(INVITEE_ID, token);

        assertThat(invited.getStatus()).isEqualTo("ACTIVE");
        assertThat(invited.getJoinedAt()).isNotNull();
        assertThat(invited.getInvitationTokenHash()).isNull();
        assertThat(invited.getInvitationExpiresAt()).isNull();

        verify(members).save(invited);
    }

    @Test
    void deleteShouldReturnConflictWhenWorkspaceCannotBeDeleted() {
        when(workspaces.findByIdAndOwnerId(
                WORKSPACE_ID,
                OWNER_ID
        )).thenReturn(Optional.of(workspace));

        doThrow(new DataIntegrityViolationException("FK violation"))
                .when(workspaces)
                .flush();

        assertThatThrownBy(() ->
                service.delete(OWNER_ID, WORKSPACE_ID)
        )
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException =
                            (ApiException) exception;

                    assertThat(apiException.getStatus())
                            .isEqualTo(HttpStatus.CONFLICT);

                    assertThat(apiException.getMessage())
                            .isEqualTo(
                                    "Workspace still has sites and cannot be deleted."
                            );
                });

        verify(workspaces).delete(workspace);
        verify(workspaces).flush();
    }
}
