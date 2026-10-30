package az.baku.managesystemback.security;

public record AuthenticatedUser(long id, String email, String role) {
}
