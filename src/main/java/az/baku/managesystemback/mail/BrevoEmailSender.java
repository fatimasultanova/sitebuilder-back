package az.baku.managesystemback.mail;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.config.BrevoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "brevo")
@ConditionalOnBooleanProperty(prefix = "app.auth", name = "email-enabled", havingValue = true)
public class BrevoEmailSender implements EmailSender {

    private static final String EMAIL_PATH = "/v3/smtp/email";
    private static final Duration FIRST_BACKOFF = Duration.ofMillis(500);
    private static final Duration SECOND_BACKOFF = Duration.ofMillis(1500);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final RestClient restClient;
    private final String apiKey;
    private final SenderAddress senderAddress;
    private final int maxRetries;

    public BrevoEmailSender(
            @Qualifier("brevoRestClient") RestClient restClient,
            BrevoProperties brevoProperties,
            AuthProperties authProperties
    ) {
        this.restClient = restClient;
        this.apiKey = validateApiKey(brevoProperties.getApiKey());
        this.senderAddress = EmailAddressParser.parse(authProperties.getFromEmail());
        this.maxRetries = Math.max(0, brevoProperties.getMaxRetries());
    }

    @Override
    public void send(EmailMessage message) {
        String recipient = EmailMasker.mask(message.toEmail());
        String type = resolveType(message);
        BrevoEmailRequest payload = toPayload(message);

        for (int attempt = 0; ; attempt++) {
            try {
                ResponseEntity<Void> response = restClient.post()
                        .uri(EMAIL_PATH)
                        .header("api-key", apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                if (response.getStatusCode().is2xxSuccessful()) {
                    return;
                }

                log.error(
                        "Brevo email send failed: type={}, status={}, recipient={}.",
                        type,
                        response.getStatusCode().value(),
                        recipient
                );
                return;
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();

                if (status == 429 && attempt < maxRetries) {
                    if (sleep(retryDelay(ex, attempt))) {
                        continue;
                    }
                }

                logFailure(status, ex.getResponseBodyAsString(), recipient, type);
                return;
            } catch (ResourceAccessException ex) {
                if (isRetryableTransportFailure(ex) && attempt < maxRetries) {
                    if (sleep(backoff(attempt))) {
                        continue;
                    }
                }

                log.error(
                        "Brevo email send failed: type={}, recipient={}, transport error={}.",
                        type,
                        recipient,
                        safeTransportError(ex)
                );
                return;
            } catch (RuntimeException ex) {
                log.error(
                        "Brevo email send failed unexpectedly: type={}, recipient={}, error={}.",
                        type,
                        recipient,
                        ex.getClass().getSimpleName()
                );
                return;
            }
        }
    }

    private BrevoEmailRequest toPayload(EmailMessage message) {
        String recipientName = message.toName() == null ? "" : message.toName();
        return new BrevoEmailRequest(
                new BrevoSender(senderAddress.name(), senderAddress.email()),
                List.of(new BrevoRecipient(message.toEmail(), recipientName)),
                message.subject(),
                message.html(),
                message.text()
        );
    }

    private void logFailure(int status, String responseBody, String recipient, String type) {
        String code = sanitize(extractJsonField(responseBody, "code"));
        String brevoMessage = sanitize(extractJsonField(responseBody, "message"));

        if (looksLikeQuotaProblem(status, code, brevoMessage)) {
            log.warn(
                    "Brevo email quota or rate limit may be exhausted (free plan allows about 300 emails/day). "
                            + "type={}, status={}, code={}, recipient={}",
                    type,
                    status,
                    code,
                    recipient
            );
        }

        String hint = hintFor(status, code, brevoMessage);
        if (hint.isEmpty()) {
            log.error(
                    "Brevo email send failed: type={}, status={}, code={}, message={}, recipient={}.",
                    type,
                    status,
                    code,
                    brevoMessage,
                    recipient
            );
        } else {
            log.error(
                    "Brevo email send failed: type={}, status={}, code={}, message={}, recipient={}. Hint: {}",
                    type,
                    status,
                    code,
                    brevoMessage,
                    recipient,
                    hint
            );
        }
    }

    private boolean looksLikeQuotaProblem(int status, String code, String message) {
        if (status == 402 || status == 429) {
            return true;
        }

        String combined = ((code == null ? "" : code) + " " + (message == null ? "" : message))
                .toLowerCase(Locale.ROOT);

        return combined.contains("quota")
                || combined.contains("limit")
                || combined.contains("credit")
                || combined.contains("plan");
    }

    private String hintFor(int status, String code, String message) {
        if (status == 401) {
            return "check BREVO_API_KEY and Brevo Security > Authorized IPs (disable IP restriction).";
        }

        if (status == 400) {
            String combined = ((code == null ? "" : code) + " " + (message == null ? "" : message))
                    .toLowerCase(Locale.ROOT);
            if (combined.contains("sender")) {
                return "sender address is not verified in Brevo.";
            }
        }

        if (status == 402 || status == 403) {
            return "check the Brevo account plan and transactional email permissions.";
        }

        if (status == 429) {
            return "Brevo is rate limiting requests; retry later or check the daily sending limit.";
        }

        return "";
    }

    private boolean isRetryableTransportFailure(ResourceAccessException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException
                    || cause instanceof UnknownHostException) {
                return true;
            }
            if (cause instanceof HttpTimeoutException) {
                return false;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Duration retryDelay(RestClientResponseException ex, int attempt) {
        String retryAfter = ex.getResponseHeaders() == null
                ? null
                : ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);

        if (retryAfter != null && !retryAfter.isBlank()) {
            String value = retryAfter.trim();

            try {
                return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
            } catch (NumberFormatException ignored) {
            }

            try {
                Instant retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                Duration delay = Duration.between(Instant.now(), retryAt);
                return delay.isNegative() ? Duration.ZERO : delay;
            } catch (DateTimeParseException ignored) {
            }
        }

        return backoff(attempt);
    }

    private Duration backoff(int attempt) {
        return attempt == 0 ? FIRST_BACKOFF : SECOND_BACKOFF;
    }

    private boolean sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String resolveType(EmailMessage message) {
        String content = (message.html() == null ? "" : message.html())
                + (message.text() == null ? "" : message.text());
        if (content.contains("/verify-email")) {
            return "verification";
        }
        if (content.contains("/reset-password")) {
            return "password-reset";
        }
        return "transactional";
    }

    private String safeTransportError(ResourceAccessException ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = sanitize(root.getMessage());
        return root.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String sanitized = EmailMasker.maskAll(value);
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "[redacted]");
        }
        sanitized = URL_PATTERN.matcher(sanitized).replaceAll("[url]");

        if (sanitized.length() > 500) {
            return sanitized.substring(0, 500) + "...";
        }

        return sanitized;
    }

    private String extractJsonField(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }

        return unescapeJson(matcher.group(1));
    }

    private String unescapeJson(String value) {
        StringBuilder result = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);

            if (current != '\\' || i + 1 >= value.length()) {
                result.append(current);
                continue;
            }

            char escaped = value.charAt(++i);
            switch (escaped) {
                case '"' -> result.append('"');
                case '\\' -> result.append('\\');
                case '/' -> result.append('/');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        try {
                            result.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            result.append("\\u");
                        }
                    } else {
                        result.append("\\u");
                    }
                }
                default -> result.append(escaped);
            }
        }

        return result.toString();
    }

    private String validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "AUTH_EMAIL_ENABLED=true with MAIL_PROVIDER=brevo requires BREVO_API_KEY to be set."
            );
        }
        return apiKey.trim();
    }

    private record BrevoSender(String name, String email) {
    }

    private record BrevoRecipient(String email, String name) {
    }

    private record BrevoEmailRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            String subject,
            String htmlContent,
            String textContent
    ) {
    }
}
