package br.dev.bielsolosos.biscraper.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "scraperDispatcherExecutor")
    Executor scraperDispatcherExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ScrapDispatch-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean(name = "notificationDispatcherExecutor")
    Executor notificationDispatcherExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("Notification-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean(name = "webhookProcessorExecutor")
    Executor webhookProcessorExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("WebhookProc-");
        executor.setVirtualThreads(true);
        return executor;
    }
}

