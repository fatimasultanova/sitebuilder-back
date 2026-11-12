package az.baku.managesystemback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Fatima Sultanova
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "billing")
public class BillingProperties {

    private String provider = "fake";
}
