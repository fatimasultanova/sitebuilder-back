package az.baku.managesystemback.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default SSL provisioner. No certificate is issued yet: the domain stays in
 * the PENDING SSL state until a real provider (Let's Encrypt, Cloudflare, ...)
 * is plugged in.
 *
 * @author Fatima Sultanova
 */
@Slf4j
@Component
public class LoggingSslProvisioner implements SslProvisioner {

    @Override
    public void provision(String hostname) {
        log.info(
                "Domain {} is connected; automatic SSL provisioning is not implemented yet, "
                        + "ssl status remains PENDING.",
                hostname
        );
    }
}
