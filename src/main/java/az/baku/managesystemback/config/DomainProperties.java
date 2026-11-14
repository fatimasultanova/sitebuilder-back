package az.baku.managesystemback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fatima Sultanova
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.domains")
public class DomainProperties {

    private String cnameTarget = "";

    private List<String> aRecords = new ArrayList<>();

    private String platformBaseDomain = "";
}
