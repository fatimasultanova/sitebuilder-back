package az.baku.managesystemback.service;

import az.baku.managesystemback.entity.DeploymentStatus;
import az.baku.managesystemback.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Marks deployments that were interrupted by an application restart as failed.
 *
 * @author Fatima Sultanova
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentStartupRecovery implements ApplicationRunner {

    private static final String STALE_MESSAGE =
            "Deployment was interrupted by an application restart.";

    private final DeploymentRepository deploymentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int recovered = deploymentRepository.markStaleAsFailed(
                DeploymentStatus.FAILED,
                List.of(DeploymentStatus.QUEUED, DeploymentStatus.BUILDING),
                STALE_MESSAGE,
                Instant.now()
        );

        if (recovered > 0) {
            log.warn("Marked {} stale deployment(s) as FAILED on startup", recovered);
        }
    }
}
