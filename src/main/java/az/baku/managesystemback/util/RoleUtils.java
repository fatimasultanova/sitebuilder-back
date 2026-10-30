package az.baku.managesystemback.util;

import java.util.Locale;
import java.util.Set;

public final class RoleUtils {

    public static final String USER = "user";
    public static final String ADMIN = "admin";

    private static final Set<String> KNOWN_ROLES = Set.of(USER, ADMIN);

    private RoleUtils() {
    }

    public static String normalize(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("User role is missing.");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        normalized = normalized.toLowerCase(Locale.ROOT);

        if (!KNOWN_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported user role: " + role);
        }
        return normalized;
    }

    public static String toAuthority(String role) {
        return "ROLE_" + normalize(role).toUpperCase(Locale.ROOT);
    }

    public static String toStoredRole(String role) {
        return toAuthority(role);
    }
}
