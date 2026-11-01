package az.baku.managesystemback.mail;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.config.BrevoProperties;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailSenderConditionsTest {

    private final RestClient restClient = mock(RestClient.class);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withBean("brevoRestClient", RestClient.class, () -> restClient);

    @Test
    void disabledEmailStartsWithBlankApiKeyAndMakesNoHttpCalls() {
        runner.withPropertyValues(
                        "app.auth.email-enabled=false",
                        "app.mail.provider=brevo",
                        "app.mail.brevo.api-key=",
                        "app.auth.from-email=Site Builder <no-reply@sitebuilder.local>"
                )
                .withUserConfiguration(LoggingEmailSender.class, EmailService.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(BrevoEmailSender.class);

                    User user = User.builder()
                            .name("Test")
                            .email("user@example.com")
                            .build();

                    context.getBean(EmailService.class).sendEmailVerificationEmail(user, "raw-token");

                    verifyNoInteractions(restClient);
                });
    }

    @Test
    void enabledEmailWithBlankApiKeyFailsStartupWithReadableMessage() {
        runner.withPropertyValues(
                        "app.auth.email-enabled=true",
                        "app.mail.provider=brevo",
                        "app.mail.brevo.api-key=",
                        "app.auth.from-email=Site Builder <no-reply@sitebuilder.local>"
                )
                .withUserConfiguration(BrevoEmailSender.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "AUTH_EMAIL_ENABLED=true with MAIL_PROVIDER=brevo requires BREVO_API_KEY to be set."
                            );
                });
    }

    @Test
    void enabledEmailWithInvalidSenderFailsStartup() {
        runner.withPropertyValues(
                        "app.auth.email-enabled=true",
                        "app.mail.provider=brevo",
                        "app.mail.brevo.api-key=xkeysib-test",
                        "app.auth.from-email=not-an-email"
                )
                .withUserConfiguration(BrevoEmailSender.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Invalid AUTH_EMAIL_FROM value. Expected 'Name <email>' or 'email'."
                            );
                });
    }

    @Test
    void invalidSenderDoesNotFailStartupWhenEmailIsDisabled() {
        runner.withPropertyValues(
                        "app.auth.email-enabled=false",
                        "app.mail.provider=brevo",
                        "app.mail.brevo.api-key=",
                        "app.auth.from-email=not-an-email"
                )
                .withUserConfiguration(LoggingEmailSender.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties({AuthProperties.class, BrevoProperties.class})
    static class PropertiesConfiguration {

        @Bean
        EmailContentBuilder emailContentBuilder(AuthProperties authProperties) {
            return new EmailContentBuilder(authProperties);
        }
    }
}
