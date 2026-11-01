package az.baku.managesystemback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private Duration sessionTtl = Duration.ofDays(7);
    private Duration passwordResetTokenTtl = Duration.ofMinutes(30);
    private Duration emailVerificationTokenTtl = Duration.ofHours(24);

    private String frontendBaseUrl = "https://manage-system-phi.vercel.app";
    private String passwordResetPath = "/reset-password";
    private String emailVerificationPath = "/verify-email";

    private boolean emailEnabled = false;
    private String fromEmail = "Site Builder <fatimasultanova.909@gmail.com>";
    private String emailLocale = "az";
    private Duration resendCooldown = Duration.ofSeconds(60);
}
