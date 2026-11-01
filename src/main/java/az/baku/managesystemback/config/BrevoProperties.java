package az.baku.managesystemback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail.brevo")
public class BrevoProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.brevo.com";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(8);
    private int maxRetries = 2;
}
