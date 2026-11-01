package az.baku.managesystemback.event;

import az.baku.managesystemback.mail.EmailMasker;

public record VerificationEmailRequested(String email, String displayName, String rawToken) {

    @Override
    public String toString() {
        return "VerificationEmailRequested[email=%s, displayName=%s]"
                .formatted(EmailMasker.mask(email), displayName);
    }
}
