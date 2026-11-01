package az.baku.managesystemback.mail;

import az.baku.managesystemback.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EmailContentBuilder {

    private static final String BRAND = "Site Builder";
    private static final String BUTTON_COLOR = "#4F46E5";

    private final AuthProperties authProperties;

    public EmailMessage verificationEmail(String toEmail, String toName, String rawToken) {
        boolean azerbaijani = isAzerbaijani();
        String link = buildLink(authProperties.getEmailVerificationPath(), rawToken);
        String expiry = formatDuration(authProperties.getEmailVerificationTokenTtl(), azerbaijani);

        String subject = azerbaijani
                ? "E-poçt ünvanınızı təsdiqləyin"
                : "Verify your email address";
        String heading = subject;
        String greeting = azerbaijani ? "Salam" : "Hello";
        String intro = azerbaijani
                ? "Site Builder hesabınızı aktivləşdirmək üçün e-poçt ünvanınızı təsdiqləyin."
                : "Please confirm your email address to activate your Site Builder account.";
        String button = azerbaijani ? "E-poçtu təsdiqlə" : "Verify email";
        String fallback = azerbaijani
                ? "Düymə işləmirsə, aşağıdakı linki brauzerinizə kopyalayın:"
                : "If the button does not work, copy this link into your browser:";
        String expiryText = azerbaijani
                ? "Bu link %s ərzində etibarlıdır.".formatted(expiry)
                : "This link expires in %s.".formatted(expiry);
        String ignore = azerbaijani
                ? "Bu e-poçtu siz istəməmisinizsə, nəzərə almayın."
                : "If you did not request this email, you can safely ignore it.";

        String text = buildText(greeting, toName, intro, link, expiryText, ignore);
        String html = buildHtml(azerbaijani ? "az" : "en", heading, greeting, toName, intro,
                button, link, fallback, expiryText, ignore);

        return new EmailMessage(toEmail, toName, subject, html, text);
    }

    public EmailMessage passwordResetEmail(String toEmail, String toName, String rawToken) {
        boolean azerbaijani = isAzerbaijani();
        String link = buildLink(authProperties.getPasswordResetPath(), rawToken);
        String expiry = formatDuration(authProperties.getPasswordResetTokenTtl(), azerbaijani);

        String subject = azerbaijani ? "Şifrənizi sıfırlayın" : "Reset your password";
        String heading = subject;
        String greeting = azerbaijani ? "Salam" : "Hello";
        String intro = azerbaijani
                ? "Şifrənizi sıfırlamaq üçün aşağıdakı düyməyə klikləyin."
                : "Click the button below to choose a new password.";
        String button = azerbaijani ? "Şifrəni sıfırla" : "Reset password";
        String fallback = azerbaijani
                ? "Düymə işləmirsə, aşağıdakı linki brauzerinizə kopyalayın:"
                : "If the button does not work, copy this link into your browser:";
        String expiryText = azerbaijani
                ? "Bu link %s ərzində etibarlıdır.".formatted(expiry)
                : "This link expires in %s.".formatted(expiry);
        String ignore = azerbaijani
                ? "Bu sorğunu siz göndərməmisinizsə, bu e-poçtu nəzərə almayın."
                : "If you did not request a password reset, you can safely ignore this email.";

        String text = buildText(greeting, toName, intro, link, expiryText, ignore);
        String html = buildHtml(azerbaijani ? "az" : "en", heading, greeting, toName, intro,
                button, link, fallback, expiryText, ignore);

        return new EmailMessage(toEmail, toName, subject, html, text);
    }

    private String buildText(String greeting, String name, String intro, String link,
                             String expiryText, String ignore) {
        return """
                %s,

                %s

                %s

                %s %s
                """.formatted(greetingLine(greeting, name), intro, link, expiryText, ignore);
    }

    private String buildHtml(String language, String heading, String greeting, String name,
                             String intro, String button, String link, String fallback,
                             String expiryText, String ignore) {
        String safeLink = HtmlUtils.htmlEscape(link);
        String safeGreeting = HtmlUtils.htmlEscape(greetingLine(greeting, name));
        String safeHeading = HtmlUtils.htmlEscape(heading);
        String safeIntro = HtmlUtils.htmlEscape(intro);
        String safeButton = HtmlUtils.htmlEscape(button);
        String safeFallback = HtmlUtils.htmlEscape(fallback);
        String safeExpiry = HtmlUtils.htmlEscape(expiryText);
        String safeIgnore = HtmlUtils.htmlEscape(ignore);

        return """
                <!DOCTYPE html>
                <html lang="%s">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F3F4F6;color:#111827;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F3F4F6;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:480px;background-color:#FFFFFF;border-radius:12px;padding:32px;box-sizing:border-box;">
                          <tr>
                            <td>
                              <p style="margin:0 0 24px;color:%s;font-size:18px;font-weight:700;">%s</p>
                              <h1 style="margin:0 0 16px;color:#111827;font-size:24px;line-height:1.3;">%s</h1>
                              <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">%s</p>
                              <p style="margin:0 0 24px;font-size:16px;line-height:1.6;">%s</p>
                              <p style="margin:0 0 24px;">
                                <a href="%s" style="display:inline-block;background-color:%s;color:#FFFFFF;text-decoration:none;padding:14px 28px;border-radius:8px;font-size:16px;font-weight:600;">%s</a>
                              </p>
                              <p style="margin:0 0 8px;color:#6B7280;font-size:14px;line-height:1.5;">%s</p>
                              <p style="margin:0 0 24px;word-break:break-all;font-size:14px;line-height:1.5;">
                                <a href="%s" style="color:%s;text-decoration:underline;">%s</a>
                              </p>
                              <p style="margin:0 0 8px;font-size:14px;line-height:1.5;">%s</p>
                              <p style="margin:0;color:#6B7280;font-size:14px;line-height:1.5;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                language,
                safeHeading,
                BUTTON_COLOR,
                HtmlUtils.htmlEscape(BRAND),
                safeHeading,
                safeGreeting,
                safeIntro,
                safeLink,
                BUTTON_COLOR,
                safeButton,
                safeFallback,
                safeLink,
                BUTTON_COLOR,
                safeLink,
                safeExpiry,
                safeIgnore
        );
    }

    private String greetingLine(String greeting, String name) {
        if (name == null || name.isBlank()) {
            return greeting + ",";
        }
        return greeting + " " + name.trim() + ",";
    }

    private String buildLink(String path, String rawToken) {
        String baseUrl = authProperties.getFrontendBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "";
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String normalizedPath = path == null || path.isBlank() ? "" : path;
        if (!normalizedPath.isEmpty() && !normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        String separator = normalizedPath.contains("?") ? "&" : "?";
        return baseUrl + normalizedPath + separator + "token=" + rawToken;
    }

    private String formatDuration(Duration duration, boolean azerbaijani) {
        if (duration == null) {
            return azerbaijani ? "bir neçə dəqiqə" : "a few minutes";
        }

        long minutes = Math.max(1, duration.toMinutes());
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours + (azerbaijani ? " saat" : " hours");
        }

        return minutes + (azerbaijani ? " dəqiqə" : " minutes");
    }

    private boolean isAzerbaijani() {
        String locale = authProperties.getEmailLocale();
        if (locale == null || locale.isBlank()) {
            return true;
        }
        return !locale.toLowerCase(Locale.ROOT).startsWith("en");
    }
}
