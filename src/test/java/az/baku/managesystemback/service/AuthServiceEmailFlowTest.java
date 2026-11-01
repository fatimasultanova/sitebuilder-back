package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AsyncMailConfiguration;
import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.dto.request.RegisterRequest;
import az.baku.managesystemback.dto.request.ResendVerificationRequest;
import az.baku.managesystemback.entity.EmailVerificationToken;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.mail.EmailContentBuilder;
import az.baku.managesystemback.mail.EmailMessage;
import az.baku.managesystemback.mail.EmailSender;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.EmailVerificationTokenRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceEmailFlowTest {

    private static final String GENERIC_VERIFICATION_MESSAGE =
            "If an account with that email exists and is not verified, a verification email has been sent.";

    private UserRepository userRepository;
    private AuthSessionRepository authSessionRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailSender emailSender;
    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authSessionRepository = mock(AuthSessionRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        emailVerificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailSender = mock(EmailSender.class);

        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runner = new ApplicationContextRunner()
                .withUserConfiguration(
                        TransactionConfiguration.class,
                        AsyncMailConfiguration.class,
                        EmailContentBuilder.class,
                        EmailService.class,
                        AuthService.class
                )
                .withBean(AuthProperties.class, this::authProperties)
                .withBean(UserRepository.class, () -> userRepository)
                .withBean(AuthSessionRepository.class, () -> authSessionRepository)
                .withBean(PasswordResetTokenRepository.class, () -> passwordResetTokenRepository)
                .withBean(EmailVerificationTokenRepository.class, () -> emailVerificationTokenRepository)
                .withBean(PasswordEncoder.class, () -> passwordEncoder)
                .withBean(EmailSender.class, () -> emailSender);
    }

    @Test
    void registerSendsEmailOnlyAfterCommit() throws Exception {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            sent.countDown();
            return null;
        }).when(emailSender).send(any(EmailMessage.class));

        runner.run(context -> {
            AuthService authService = context.getBean(AuthService.class);
            String response = authService.register(registerRequest());

            assertThat(response).isEqualTo(
                    "Account created successfully. Please check your email to verify your account."
            );
            assertThat(sent.await(3, TimeUnit.SECONDS)).isTrue();

            ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
            verify(emailSender).send(captor.capture());
            assertThat(captor.getValue().toEmail()).isEqualTo("new@example.com");
            assertThat(captor.getValue().subject()).isNotBlank();
        });
    }

    @Test
    void registerSendsNothingWhenOuterTransactionRollsBack() throws Exception {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            sent.countDown();
            return null;
        }).when(emailSender).send(any(EmailMessage.class));

        runner.run(context -> {
            AuthService authService = context.getBean(AuthService.class);
            PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {
                authService.register(registerRequest());
                assertThat(sent.getCount()).isEqualTo(1);
                status.setRollbackOnly();
            });

            assertThat(sent.await(300, TimeUnit.MILLISECONDS)).isFalse();
            verifyNoInteractions(emailSender);
        });
    }

    @Test
    void registerResponseIsUnaffectedWhenEmailSenderThrows() throws Exception {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        CountDownLatch attempted = new CountDownLatch(1);
        doAnswer(invocation -> {
            attempted.countDown();
            throw new IllegalStateException("simulated sender failure");
        }).when(emailSender).send(any(EmailMessage.class));

        runner.run(context -> {
            AuthService authService = context.getBean(AuthService.class);

            assertThatCode(() -> assertThat(authService.register(registerRequest())).isNotBlank())
                    .doesNotThrowAnyException();
            assertThat(attempted.await(3, TimeUnit.SECONDS)).isTrue();
        });
    }

    @Test
    void resendVerificationInsideCooldownReturnsSameMessageAndSendsNothing() throws Exception {
        User user = enabledUnverifiedUser();
        EmailVerificationToken recentToken = EmailVerificationToken.builder()
                .user(user)
                .createdAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .build();

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(recentToken));

        runner.run(context -> {
            AuthService authService = context.getBean(AuthService.class);

            String response = authService.resendVerification(
                    new ResendVerificationRequest("user@example.com")
            );

            assertThat(response).isEqualTo(GENERIC_VERIFICATION_MESSAGE);
            verify(emailVerificationTokenRepository, never()).deleteAllByUser(any());
            verify(emailVerificationTokenRepository, never()).save(any());
            Thread.sleep(300);
            verifyNoInteractions(emailSender);
        });
    }

    @Test
    void resendVerificationOutsideCooldownSendsEmail() throws Exception {
        User user = enabledUnverifiedUser();
        EmailVerificationToken oldToken = EmailVerificationToken.builder()
                .user(user)
                .createdAt(Instant.now().minus(Duration.ofMinutes(2)))
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .build();

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(oldToken));

        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            sent.countDown();
            return null;
        }).when(emailSender).send(any(EmailMessage.class));

        runner.run(context -> {
            AuthService authService = context.getBean(AuthService.class);

            String response = authService.resendVerification(
                    new ResendVerificationRequest("user@example.com")
            );

            assertThat(response).isEqualTo(GENERIC_VERIFICATION_MESSAGE);
            assertThat(sent.await(3, TimeUnit.SECONDS)).isTrue();
            verify(emailSender).send(any(EmailMessage.class));
        });
    }

    private AuthProperties authProperties() {
        AuthProperties properties = new AuthProperties();
        properties.setFrontendBaseUrl("http://localhost:3000");
        properties.setEmailVerificationTokenTtl(Duration.ofHours(24));
        properties.setPasswordResetTokenTtl(Duration.ofMinutes(30));
        properties.setResendCooldown(Duration.ofSeconds(60));
        return properties;
    }

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .name("New User")
                .email("new@example.com")
                .password("secret1")
                .build();
    }

    private User enabledUnverifiedUser() {
        return User.builder()
                .name("Test")
                .surname("User")
                .email("user@example.com")
                .password("encoded-password")
                .enabled(true)
                .emailVerified(false)
                .build();
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

                @Override
                protected Object doGetTransaction() {
                    return new TransactionState(active.get());
                }

                @Override
                protected boolean isExistingTransaction(Object transaction) {
                    return ((TransactionState) transaction).existing();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                    active.set(true);
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                    active.remove();
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                    active.remove();
                }
            };
        }

        record TransactionState(boolean existing) {
        }
    }
}
