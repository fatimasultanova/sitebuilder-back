package az.baku.managesystemback.service;

import java.util.List;

/**
 * Resolves DNS records for custom domain verification.
 *
 * @author Fatima Sultanova
 */
public interface DnsResolver {

    List<String> lookupTxt(String hostname);

    List<String> lookupCname(String hostname);

    List<String> lookupA(String hostname);
}
