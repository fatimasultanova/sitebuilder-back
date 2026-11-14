package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.SecurityEvent;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String PASSWORD_CHANGED = "password_changed";
    public static final String PASSWORD_CHANGE_FAILED = "password_change_failed";
    public static final String EMAIL_VERIFIED = "email_verified";
    public static final String VERIFICATION_EMAIL_SENT = "verification_email_sent";
    public static final String SESSION_REVOKED = "session_revoked";
    public static final String RECOVERY_REQUESTED = "recovery_requested";

    private static final int IP_MAX_LENGTH = 45;
    private static final int USER_AGENT_MAX_LENGTH = 512;
    private static final int DETAIL_MAX_LENGTH = 255;

    private final SecurityEventRepository securityEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            User user,
            String type,
            String ipAddress,
            String userAgent,
            String detail,
            UUID sessionPublicId
    ) {
        if (user == null) {
            return;
        }

        try {
            SecurityEvent event = SecurityEvent.builder()
                    .user(user)
                    .type(type)
                    .occurredAt(Instant.now())
                    .ipAddress(truncate(ipAddress, IP_MAX_LENGTH))
                    .userAgent(truncate(userAgent, USER_AGENT_MAX_LENGTH))
                    .detail(truncate(detail, DETAIL_MAX_LENGTH))
                    .sessionPublicId(sessionPublicId)
                    .build();

            securityEventRepository.saveAndFlush(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to record security event {}: {}", type, ex.getClass().getSimpleName());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
