package com.helpdeskai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Enables @Async annotation and configures thread pool for async operations.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size (minimum threads)
        executor.setCorePoolSize(5);

        // Maximum pool size
        executor.setMaxPoolSize(10);

        // Queue capacity (pending tasks before creating new threads)
        executor.setQueueCapacity(100);

        // Thread name prefix (for easier debugging)
        executor.setThreadNamePrefix("Async-");

        // Rejection policy: caller runs the task in the caller's thread if queue is full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Maximum time to wait for tasks on shutdown (30 seconds)
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Async executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                 executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
