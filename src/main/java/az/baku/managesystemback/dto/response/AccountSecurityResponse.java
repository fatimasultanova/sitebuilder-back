package az.baku.managesystemback.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AccountSecurityResponse(
        Account account,
        EmailVerification emailVerification,
        List<Object> connectedAccounts
) {

    public record Account(
            String displayName,
            String email,
            String status,
            Instant lastSignInAt,
            String lastSignInDevice
    ) {
    }

    public record EmailVerification(boolean verified, LocalDateTime verifiedAt) {
    }
}
