package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.entity.WorkspaceMember;

import java.time.Instant;
import java.util.Locale;

public record MemberResponse(
        String id,
        String userId,
        String email,
        String name,
        String role,
        String status,
        Instant invitedAt,
        Instant joinedAt
) {

    public static MemberResponse from(WorkspaceMember member) {
        String normalizedStatus = normalize(member.getStatus());

        Instant joinedAt = member.getJoinedAt();

        if ("active".equals(normalizedStatus) && joinedAt == null) {
            joinedAt = member.getCreatedAt();
        }

        return new MemberResponse(
                Long.toString(member.getId()),
                Long.toString(member.getUser().getId()),
                member.getUser().getEmail(),
                member.getUser().getName(),
                normalizeRole(member.getRole()),
                normalizedStatus,
                member.getCreatedAt(),
                joinedAt
        );
    }

    public static MemberResponse owner(User owner) {
        return new MemberResponse(
                "owner",
                Long.toString(owner.getId()),
                owner.getEmail(),
                owner.getName(),
                "owner",
                "active",
                null,
                null
        );
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        if ("MEMBER".equalsIgnoreCase(role)) {
            return "viewer";
        }

        return role.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null
                ? null
                : value.toLowerCase(Locale.ROOT);
    }
}
