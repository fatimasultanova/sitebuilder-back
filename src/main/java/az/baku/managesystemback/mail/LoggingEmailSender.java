package az.baku.managesystemback.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@ConditionalOnBooleanProperty(prefix = "app.auth", name = "email-enabled", havingValue = false, matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Pattern LINK_PATTERN = Pattern.compile("https?://\\S+");

    @Override
    public void send(EmailMessage message) {
        log.info(
                "Email delivery is disabled (AUTH_EMAIL_ENABLED=false). type={}, recipient={}, link={}",
                resolveType(message),
                EmailMasker.mask(message.toEmail()),
                extractLink(message)
        );
    }

    private String extractLink(EmailMessage message) {
        String text = message.text() == null ? "" : message.text();
        Matcher matcher = LINK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "(no link found)";
    }

    private String resolveType(EmailMessage message) {
        String content = (message.html() == null ? "" : message.html())
                + (message.text() == null ? "" : message.text());
        if (content.contains("/verify-email")) {
            return "verification";
        }
        if (content.contains("/reset-password")) {
            return "password-reset";
        }
        return "transactional";
    }
}
