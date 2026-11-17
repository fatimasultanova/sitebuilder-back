package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.User;

public record ProfileResponse(String id, String name, String surname, String email, String role, String avatarUrl) {
    public static ProfileResponse from(User u) {
        return new ProfileResponse(Long.toString(u.getId()), u.getName(), u.getSurname(), u.getEmail(), u.getRole(), u.getAvatarUrl());
    }
}
