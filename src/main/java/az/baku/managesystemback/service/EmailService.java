package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.event.PasswordResetEmailRequested;
import az.baku.managesystemback.event.VerificationEmailRequested;
import az.baku.managesystemback.mail.EmailContentBuilder;
import az.baku.managesystemback.mail.EmailMasker;
import az.baku.managesystemback.mail.EmailMessage;
import az.baku.managesystemback.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final EmailContentBuilder contentBuilder;
    private final EmailSender emailSender;

    public void sendPasswordResetEmail(User user, String rawToken) {
        EmailMessage message = contentBuilder.passwordResetEmail(
                user.getEmail(),
                displayName(user),
                rawToken
        );
        emailSender.send(message);
    }

    public void sendEmailVerificationEmail(User user, String rawToken) {
        EmailMessage message = contentBuilder.verificationEmail(
                user.getEmail(),
                displayName(user),
                rawToken
        );
        emailSender.send(message);
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationEmailRequested(VerificationEmailRequested event) {
        try {
            EmailMessage message = contentBuilder.verificationEmail(
                    event.email(),
                    event.displayName(),
                    event.rawToken()
            );
            emailSender.send(message);
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to send verification email to {}: {}.",
                    EmailMasker.mask(event.email()),
                    ex.getClass().getSimpleName()
            );
        }
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmailRequested(PasswordResetEmailRequested event) {
        try {
            EmailMessage message = contentBuilder.passwordResetEmail(
                    event.email(),
                    event.displayName(),
                    event.rawToken()
            );
            emailSender.send(message);
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to send password reset email to {}: {}.",
                    EmailMasker.mask(event.email()),
                    ex.getClass().getSimpleName()
            );
        }
    }

    private String displayName(User user) {
        if (user.getSurname() == null || user.getSurname().isBlank()) {
            return user.getName();
        }
        return user.getName() + " " + user.getSurname();
    }
}
