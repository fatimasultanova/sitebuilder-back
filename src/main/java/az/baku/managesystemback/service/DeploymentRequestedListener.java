package az.baku.managesystemback.service;

import az.baku.managesystemback.event.DeploymentRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Fatima Sultanova
 */
@Slf4j
@Component
public class DeploymentRequestedListener {

    private static final String SCHEDULING_FAILED_MESSAGE =
            "Deployment could not be started. Please try again.";

    private final ThreadPoolTaskExecutor deploymentTaskExecutor;
    private final DeploymentPipeline deploymentPipeline;

    public DeploymentRequestedListener(
            @Qualifier("deploymentTaskExecutor") ThreadPoolTaskExecutor deploymentTaskExecutor,
            DeploymentPipeline deploymentPipeline
    ) {
        this.deploymentTaskExecutor = deploymentTaskExecutor;
        this.deploymentPipeline = deploymentPipeline;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeploymentRequested(DeploymentRequestedEvent event) {
        try {
            deploymentTaskExecutor.execute(() -> deploymentPipeline.process(event.deploymentId()));
        } catch (TaskRejectedException ex) {
            log.error("Deployment task for {} was rejected", event.deploymentId(), ex);
            deploymentPipeline.markFailedIfPending(event.deploymentId(), SCHEDULING_FAILED_MESSAGE);
        }
    }
}
