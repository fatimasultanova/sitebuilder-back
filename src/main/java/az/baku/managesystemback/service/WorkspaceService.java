package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.*;
import az.baku.managesystemback.dto.response.*;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.Workspace;
import az.baku.managesystemback.entity.WorkspaceMember;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import az.baku.managesystemback.event.WorkspaceInvitationRequested;
import az.baku.managesystemback.util.TokenUtils;

import java.time.Instant;
import java.time.Duration;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<WorkspaceResponse> list(long uid, int page, int size) {
        return workspaces.findVisibleToUser(uid, PageRequest.of(page, size, Sort.by("id").descending())).map(WorkspaceResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(long uid, long id) {
        return WorkspaceResponse.from(visible(uid, id));
    }

    @Transactional
    public WorkspaceResponse create(long uid, WorkspaceRequest r) {
        User u = user(uid);
        return WorkspaceResponse.from(workspaces.save(Workspace.builder().name(r.name().trim()).owner(u).build()));
    }

    @Transactional
    public WorkspaceResponse update(long uid, long id, WorkspaceRequest r) {
        Workspace w = owned(uid, id);
        w.setName(r.name().trim());
        return WorkspaceResponse.from(workspaces.save(w));
    }

    @Transactional
    public void delete(long uid, long id) {
        workspaces.delete(owned(uid, id));
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> listMembers(long uid, long id, int page, int size) {
        visible(uid, id);
        return members.findAllByWorkspaceIdAndStatus(id, "ACTIVE", PageRequest.of(page, size, Sort.by("id").ascending())).map(MemberResponse::from);
    }
    @Transactional(readOnly = true)
    public MemberResponse getMember(long userId, long workspaceId, long memberId) {
        visible(userId, workspaceId);

        WorkspaceMember member = members.findById(memberId)
                .filter(candidate ->
                        candidate.getWorkspace().getId() == workspaceId
                                && "ACTIVE".equals(candidate.getStatus())
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Workspace member not found."
                ));

        return MemberResponse.from(member);
    }

    @Transactional
    public String invite(long uid, long id, InviteMemberRequest r) {
        Workspace w = owned(uid, id);
        User invitee = users.findByEmailIgnoreCase(r.email().trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invitee must have an account before invitation."));
        if (w.getOwner().getId() == invitee.getId() || members.existsByWorkspaceIdAndUserId(id, invitee.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "User is already associated with this workspace.");
        String raw = TokenUtils.generateToken();
        members.save(WorkspaceMember.builder().workspace(w).user(invitee).role(r.role() == null ? "MEMBER" : r.role()).status("INVITED").invitationTokenHash(TokenUtils.sha256(raw)).invitationExpiresAt(Instant.now().plus(Duration.ofDays(7))).build());
        events.publishEvent(new WorkspaceInvitationRequested(invitee.getEmail(), w.getName(), raw));
        notificationService.createForUser(
                invitee.getId(),
                "system",
                "info",
                "Workspace invitation",
                "You were invited to workspace \"" + w.getName() + "\"."
        );
        return "Invitation created.";
    }

    @Transactional
    public MemberResponse role(long uid, long id, long memberId, ChangeMemberRoleRequest r) {
        owned(uid, id);
        WorkspaceMember m = members.findById(memberId).filter(x -> x.getWorkspace().getId() == id && x.getStatus().equals("ACTIVE")).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace member not found."));
        m.setRole(r.role());
        return MemberResponse.from(members.save(m));
    }

    @Transactional
    public void remove(long uid, long id, long memberId) {
        owned(uid, id);
        WorkspaceMember m = members.findById(memberId).filter(x -> x.getWorkspace().getId() == id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace member not found."));
        members.delete(m);
    }

    @Transactional
    public void resend(long uid, long workspaceId, long memberId) {
        Workspace w = owned(uid, workspaceId);
        WorkspaceMember m = members.findById(memberId).filter(x -> x.getWorkspace().getId() == workspaceId && x.getStatus().equals("INVITED")).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invitation not found."));
        String raw = TokenUtils.generateToken();
        m.setInvitationTokenHash(TokenUtils.sha256(raw));
        m.setInvitationExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        members.save(m);
        events.publishEvent(new WorkspaceInvitationRequested(m.getUser().getEmail(), w.getName(), raw));
    }

    @Transactional
    public void accept(long uid, String token) {
        WorkspaceMember m = members.findByInvitationTokenHash(TokenUtils.sha256(token.trim())).filter(x -> x.getStatus().equals("INVITED")).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invitation token is invalid or expired."));
        if (m.getUser().getId() != uid || m.getInvitationExpiresAt() == null || !m.getInvitationExpiresAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invitation token is invalid or expired.");
        m.setStatus("ACTIVE");
        m.setInvitationTokenHash(null);
        m.setInvitationExpiresAt(null);
        members.save(m);
    }

    private Workspace visible(long uid, long id) {
        return workspaces.findVisibleByIdAndUser(id, uid).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace not found."));
    }

    private Workspace owned(long uid, long id) {
        return workspaces.findByIdAndOwnerId(id, uid).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace not found."));
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}


