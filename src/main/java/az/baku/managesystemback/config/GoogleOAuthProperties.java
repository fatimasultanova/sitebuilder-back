package az.baku.managesystemback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.google")
public class GoogleOAuthProperties {
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:3000/auth/callback";
}

