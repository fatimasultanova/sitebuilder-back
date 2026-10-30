package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AuthProperties authProperties;

    public void sendPasswordResetEmail(User user, String rawToken) {
        String link = buildLink(authProperties.getPasswordResetPath(), rawToken);
        String subject = "Reset your password";
        String body = """
                Hello %s,

                We received a request to reset your password. Use the link below to choose a new password:

                %s

                This link expires in %d minutes. If you did not request a password reset, you can safely ignore this email.
                """.formatted(
                displayName(user),
                link,
                authProperties.getPasswordResetTokenTtl().toMinutes()
        );

        send(user.getEmail(), subject, body, link);
    }

    public void sendEmailVerificationEmail(User user, String rawToken) {
        String link = buildLink(authProperties.getEmailVerificationPath(), rawToken);
        String subject = "Verify your email address";
        String body = """
                Hello %s,

                Please confirm your email address by opening the link below:

                %s

                This link expires in %d hours. If you did not create an account, you can safely ignore this email.
                """.formatted(
                displayName(user),
                link,
                authProperties.getEmailVerificationTokenTtl().toHours()
        );

        send(user.getEmail(), subject, body, link);
    }

    private void send(String recipient, String subject, String body, String sensitiveLink) {
        if (!authProperties.isEmailEnabled()) {
            log.warn("Email delivery is disabled. {} email for {}: {}", subject, recipient, sensitiveLink);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Email delivery is enabled, but no JavaMailSender is configured. {} email was not sent to {}.",
                    subject, recipient);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.getFromEmail());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send {} email to {}.", subject, recipient, ex);
        }
    }

    private String buildLink(String path, String rawToken) {
        String baseUrl = authProperties.getFrontendBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String separator = normalizedPath.contains("?") ? "&" : "?";
        return baseUrl + normalizedPath + separator + "token=" + rawToken;
    }

    private String displayName(User user) {
        if (user.getSurname() == null || user.getSurname().isBlank()) {
            return user.getName();
        }
        return user.getName() + " " + user.getSurname();
    }
}
