package az.baku.managesystemback.mail;

public record EmailMessage(
        String toEmail,
        String toName,
        String subject,
        String html,
        String text
) {
}
