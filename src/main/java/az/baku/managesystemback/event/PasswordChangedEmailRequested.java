package az.baku.managesystemback.event;

import az.baku.managesystemback.mail.EmailMasker;

public record PasswordChangedEmailRequested(String email, String displayName) {

    @Override
    public String toString() {
        return "PasswordChangedEmailRequested[email=%s, displayName=%s]"
                .formatted(EmailMasker.mask(email), displayName);
    }
}
