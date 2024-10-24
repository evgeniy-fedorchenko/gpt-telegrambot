package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.configuration.properties.ExecutorProperties;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

@EnableAsync
@EnableScheduling
@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorsConfiguration {

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(ExecutorProperties properties) {
        int queueCapacity = properties.getQueueCapacity();
        int keepAliveSeconds = properties.getKeepAliveSeconds();
        int awaitTerminationSeconds = properties.getAwaitTerminationSeconds();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(ExecutorProperties.POOL_SIZE);
        executor.setMaxPoolSize(properties.getPoolSizeMultiplierForMaxPoolSize() * ExecutorProperties.POOL_SIZE);
        executor.setQueueCapacity(queueCapacity < 0 ? Integer.MAX_VALUE : properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setKeepAliveSeconds(keepAliveSeconds < 0 ? Integer.MAX_VALUE : keepAliveSeconds);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds < 0 ? Integer.MAX_VALUE : awaitTerminationSeconds);
        executor.setTaskDecorator(mdcDecorator::apply);

        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorService executorOfVirtual() {
        return Executors.newThreadPerTaskExecutor(
                srcRunnable -> Thread.ofVirtual().unstarted(mdcDecorator.apply(srcRunnable))
        );
    }

    public static ScheduledExecutorService singleThreadScheduler() {
        return Executors.newScheduledThreadPool(1,
                srcRunnable -> Thread.ofPlatform().unstarted(mdcDecorator.apply(srcRunnable))
        );
    }

    private static final Function<Runnable, Runnable> mdcDecorator = srcRunnable -> {
        Map<String, String> parentContext = MDC.getCopyOfContextMap();
        return () -> {
            if (parentContext != null) {
                MDC.setContextMap(parentContext);
            }
            try {
                srcRunnable.run();
            } finally {
                MDC.clear();
            }
        };
    };
}
