package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.WorkspaceMember;

public record MemberResponse(String id, String userId, String email, String name, String role, String status) {
    public static MemberResponse from(WorkspaceMember m) {
        return new MemberResponse(Long.toString(m.getId()), Long.toString(m.getUser().getId()), m.getUser().getEmail(), m.getUser().getName(), m.getRole(), m.getStatus());
    }
}
