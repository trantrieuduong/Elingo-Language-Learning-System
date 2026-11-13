package com.elingo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

    private final ThreadPoolProperties threadPoolProperties;

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        var emailConfig = threadPoolProperties.async().email();
        executor.setCorePoolSize(emailConfig.corePoolSize());
        executor.setMaxPoolSize(emailConfig.maxPoolSize());
        executor.setQueueCapacity(emailConfig.queueCapacity());
        executor.setThreadNamePrefix("email-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean(name = "gamificationExecutor")
    public Executor gamificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        var gamificationConfig = threadPoolProperties.async().gamification();
        executor.setCorePoolSize(gamificationConfig.corePoolSize());
        executor.setMaxPoolSize(gamificationConfig.maxPoolSize());
        executor.setQueueCapacity(gamificationConfig.queueCapacity());
        executor.setThreadNamePrefix("gamification-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
