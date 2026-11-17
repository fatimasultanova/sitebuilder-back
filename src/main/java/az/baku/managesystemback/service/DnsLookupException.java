package az.baku.managesystemback.service;

/**
 * Raised when a DNS lookup cannot be completed (timeout, network failure, etc.).
 *
 * @author Fatima Sultanova
 */
public class DnsLookupException extends RuntimeException {

    public DnsLookupException(String message) {
        super(message);
    }

    public DnsLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
