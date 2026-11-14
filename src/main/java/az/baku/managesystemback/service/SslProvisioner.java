package az.baku.managesystemback.service;

/**
 * Provisions TLS certificates for connected custom domains.
 *
 * @author Fatima Sultanova
 */
public interface SslProvisioner {

    void provision(String hostname);
}
