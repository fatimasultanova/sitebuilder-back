package az.baku.managesystemback.mail;

import az.baku.managesystemback.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentBuilderTest {

    @Test
    void buildsVerificationEmailInAzerbaijaniWithEscapedNameAndConfiguredTtl() {
        AuthProperties properties = new AuthProperties();
        properties.setFrontendBaseUrl("https://frontend.example.com/");
        properties.setEmailVerificationTokenTtl(Duration.ofHours(24));
        properties.setEmailLocale("az");

        EmailContentBuilder builder = new EmailContentBuilder(properties);
        EmailMessage message = builder.verificationEmail(
                "user@example.com",
                "<script>alert('x')</script>",
                "raw-token"
        );

        assertThat(message.subject()).isEqualTo("E-poçt ünvanınızı təsdiqləyin");
        assertThat(message.html())
                .contains("https://frontend.example.com/verify-email?token=raw-token")
                .contains("max-width:480px")
                .contains("#4F46E5")
                .contains("&lt;script&gt;")
                .doesNotContain("<script>");
        assertThat(message.text())
                .contains("https://frontend.example.com/verify-email?token=raw-token")
                .contains("24 saat");
    }

    @Test
    void buildsPasswordResetEmailInEnglishWithConfiguredTtl() {
        AuthProperties properties = new AuthProperties();
        properties.setFrontendBaseUrl("https://frontend.example.com");
        properties.setPasswordResetTokenTtl(Duration.ofMinutes(30));
        properties.setEmailLocale("en");

        EmailContentBuilder builder = new EmailContentBuilder(properties);
        EmailMessage message = builder.passwordResetEmail("user@example.com", "Jane", "raw-token");

        assertThat(message.subject()).isEqualTo("Reset your password");
        assertThat(message.html())
                .contains("https://frontend.example.com/reset-password?token=raw-token")
                .contains("This link expires in 30 minutes.");
        assertThat(message.text())
                .contains("https://frontend.example.com/reset-password?token=raw-token")
                .contains("30 minutes")
                .contains("If you did not request a password reset");
    }
}
