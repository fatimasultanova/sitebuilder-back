package az.baku.managesystemback.security;

import java.util.UUID;

public record AuthenticatedUser(long id, String email, String role, UUID sessionPublicId) {

    public AuthenticatedUser(long id, String email, String role) {
        this(id, email, role, null);
    }
}
