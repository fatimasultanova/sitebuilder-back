package az.baku.managesystemback.util;

import az.baku.managesystemback.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword, String fieldName) {
        if (rawPassword == null
                || rawPassword.length() < MIN_LENGTH
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation failed.",
                    Map.of(fieldName, List.of("Password must be between 8 and 72 characters"))
            );
        }
    }

    public static void ensureNotSameAsCurrent(
            String rawPassword,
            String encodedCurrentPassword,
            PasswordEncoder encoder,
            String fieldName
    ) {
        if (encodedCurrentPassword != null
                && encoder.matches(rawPassword, encodedCurrentPassword)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation failed.",
                    Map.of(fieldName, List.of("New password must be different from current password"))
            );
        }
    }
}
