package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.*;
import az.baku.managesystemback.repository.*;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.Map;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

/** Real SQL/JPA transactions against an isolated PostgreSQL-compatible H2 database. */
@SpringJUnitConfig(ActivityPersistenceTest.Config.class)
class ActivityPersistenceTest {
    @Autowired ActivityService activity;
    @Autowired PlatformTransactionManager transactions;

    @Test void migrationPersistsCommittedEventsIsolatesOwnersAndRollsBackFailures() {
        var site = Site.builder().id(42).projectName("Test")
                .owner(User.builder().id(7).name("Owner").build()).build();
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            activity.record(site, "site.created", "Rolled back.");
            status.setRollbackOnly();
        });
        assertThat(activity.list(7, 0, 100)).isEmpty();
        tx.executeWithoutResult(status -> activity.record(site, "site.created", "Committed."));
        assertThat(activity.list(7, 0, 100)).singleElement().satisfies(row ->
                assertThat(row).containsEntry("summary", "Committed."));
        assertThat(activity.list(8, 0, 100)).isEmpty();
        assertThat(activity.list(7, 1, 1)).isEmpty();
    }

    @Configuration @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            var ds = new DriverManagerDataSource("jdbc:h2:mem:activity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
            var jdbc = new JdbcTemplate(ds);
            jdbc.execute("CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS \"sitebuilder-data\"");
            jdbc.execute("CREATE TABLE \"sitebuilder-data\".users(id BIGINT PRIMARY KEY)");
            jdbc.execute("INSERT INTO \"sitebuilder-data\".users VALUES (7), (8)");
            new ResourceDatabasePopulator(new ClassPathResource("activity_migration.sql")).execute(ds);
            return ds;
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setManagedTypes(PersistenceManagedTypes.of(ActivityEvent.class.getName()));
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ActivityEventRepository eventRepository(EntityManagerFactory factory) {
            return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
                    .getRepository(ActivityEventRepository.class);
        }
        @Bean ActivityService activityService(ActivityEventRepository events) {
            return new ActivityService(events, mock(SiteRepository.class),
                    mock(DeploymentRepository.class), mock(PlanLimitService.class));
        }
    }
}
