package az.baku.managesystemback;

import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.repository.DeploymentRepository;
import az.baku.managesystemback.repository.EmailVerificationTokenRepository;
import az.baku.managesystemback.repository.InvoiceRepository;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import az.baku.managesystemback.repository.PasswordResetTokenRepository;
import az.baku.managesystemback.repository.PaymentMethodRepository;
import az.baku.managesystemback.repository.PaymentRepository;
import az.baku.managesystemback.repository.PaymentSessionRepository;
import az.baku.managesystemback.repository.PlanRepository;
import az.baku.managesystemback.repository.SecurityEventRepository;
import az.baku.managesystemback.repository.SiteDomainRepository;
import az.baku.managesystemback.repository.SiteRepository;
import az.baku.managesystemback.repository.SubscriptionRepository;
import az.baku.managesystemback.repository.TemplateRepository;
import az.baku.managesystemback.repository.UserRepository;
import az.baku.managesystemback.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import az.baku.managesystemback.repository.NotificationRepository;
import az.baku.managesystemback.repository.OAuthStateRepository;
import az.baku.managesystemback.repository.SupportTicketRepository;
import az.baku.managesystemback.repository.WorkspaceMemberRepository;
import az.baku.managesystemback.service.SecurityEventService;
import az.baku.managesystemback.repository.NotificationPreferencesRepository;
import az.baku.managesystemback.repository.SecurityEventRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@Import(ManageSystemBackApplicationTests.TestTransactionConfiguration.class)
class ManageSystemBackApplicationTests {
    @MockitoBean
    private az.baku.managesystemback.repository.ActivityEventRepository activityEventRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthSessionRepository authSessionRepository;

    @MockitoBean
    private SecurityEventService securityEventService;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @MockitoBean
    private SiteRepository siteRepository;

    @MockitoBean
    private SiteDomainRepository siteDomainRepository;

    @MockitoBean
    private DeploymentRepository deploymentRepository;

    @MockitoBean
    private WorkspaceRepository workspaceRepository;

    @MockitoBean
    private TemplateRepository templateRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private SupportTicketRepository supportTicketRepository;

    @MockitoBean
    private WorkspaceMemberRepository workspaceMemberRepository;

    @MockitoBean
    private OAuthStateRepository oauthStateRepository;

    @MockitoBean
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @MockitoBean
    private PlanRepository planRepository;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private PaymentSessionRepository paymentSessionRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    @MockitoBean
    private PaymentMethodRepository paymentMethodRepository;

    @MockitoBean
    private SecurityEventRepository securityEventRepository;

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestTransactionConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }
}
