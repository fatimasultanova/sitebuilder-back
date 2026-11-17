package az.baku.managesystemback.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * @author Fatima Sultanova
 */
@Configuration
@Slf4j
public class DeploymentAsyncConfiguration {

    @Bean(name = "deploymentTaskExecutor")
    public ThreadPoolTaskExecutor deploymentTaskExecutor() {
        RejectedExecutionHandler rejectedHandler = (task, executor) ->
                log.error("Deployment task queue is full; rejecting deployment task.");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("deployment-");
        executor.setRejectedExecutionHandler(rejectedHandler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
