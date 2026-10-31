package az.baku.managesystemback.mail;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.config.BrevoProperties;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoEmailSenderTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String API_KEY = "test-api-key";
    private static final String RAW_TOKEN = "secret-token";

    private MockRestServiceServer server;
    private BrevoEmailSender sender;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger senderLogger;

    @BeforeEach
    void setUp() {
        BrevoProperties properties = new BrevoProperties();
        properties.setApiKey(API_KEY);
        properties.setBaseUrl("https://api.brevo.com");
        properties.setMaxRetries(2);

        AuthProperties authProperties = new AuthProperties();
        authProperties.setFromEmail("Site Builder <verified-sender@example.com>");
        authProperties.setFrontendBaseUrl("https://frontend.example.com");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.brevo.com");
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoEmailSender(builder.build(), properties, authProperties);

        senderLogger = (Logger) LoggerFactory.getLogger(BrevoEmailSender.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        senderLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        senderLogger.detachAppender(logAppender);
        server.verify();
    }

    @Test
    void sendsExpectedBrevoRequestShape() {
        server.expect(requestTo(BREVO_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.name").value("Site Builder"))
                .andExpect(jsonPath("$.sender.email").value("verified-sender@example.com"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.to[0].name").value("Test User"))
                .andExpect(jsonPath("$.subject").value("Verify your email address"))
                .andExpect(jsonPath("$.htmlContent").value(containsString("?token=" + RAW_TOKEN)))
                .andExpect(jsonPath("$.textContent").value(containsString("?token=" + RAW_TOKEN)))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"x\"}"));

        sender.send(verificationMessage());

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void treatsCreatedResponseAsSuccess() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"x\"}"));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @Test
    void retriesOnceAfterTooManyRequests() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.RETRY_AFTER, "0")
                        .body("{\"code\":\"too_many_requests\",\"message\":\"rate limited\"}"));
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"x\"}"));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @Test
    void retriesConnectionFailure() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withException(new ConnectException("connection refused")));
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"x\"}"));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotRetryReadTimeout() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withException(new HttpTimeoutException("read timed out")));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotRetryServerError() {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"internal_error\",\"message\":\"server error\"}"));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401})
    void doesNotRetryClientErrors(int status) {
        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"client_error\",\"message\":\"sender rejected\"}"));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();
    }

    @Test
    void finalFailureDoesNotThrowAndLogsNoSecrets() {
        String responseBody = """
                {"code":"invalid_sender","message":"Sender failed for user@example.com with key %s and link https://frontend.example.com/verify-email?token=%s"}
                """.formatted(API_KEY, RAW_TOKEN);

        server.expect(requestTo(BREVO_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        assertThatCode(() -> sender.send(verificationMessage()))
                .doesNotThrowAnyException();

        String logs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertThat(logs).isNotEmpty();
        assertThat(logs).doesNotContain(RAW_TOKEN);
        assertThat(logs).doesNotContain("?token=");
        assertThat(logs).doesNotContain("https://frontend.example.com");
        assertThat(logs).doesNotContain(API_KEY);
        assertThat(logs).doesNotContain("user@example.com");
        assertThat(logs).contains("u***@example.com");
    }

    private EmailMessage verificationMessage() {
        String link = "https://frontend.example.com/verify-email?token=" + RAW_TOKEN;
        return new EmailMessage(
                "user@example.com",
                "Test User",
                "Verify your email address",
                "<html><body><a href=\"" + link + "\">Verify</a></body></html>",
                "Verify your email address: " + link
        );
    }
}
