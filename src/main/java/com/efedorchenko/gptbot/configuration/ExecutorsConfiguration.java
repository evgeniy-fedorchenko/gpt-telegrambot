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
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(ExecutorProperties.POOL_SIZE);
        executor.setMaxPoolSize(properties.getPoolSizeMultiplierForMaxPoolSize() * ExecutorProperties.POOL_SIZE);
        executor.setQueueCapacity(properties.getQueueCapacity() < 0 ? Integer.MAX_VALUE : properties.getQueueCapacity());

        executor.setThreadNamePrefix(properties.getThreadNamePrefix());

        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds() < 0 ? Integer.MAX_VALUE : properties.getKeepAliveSeconds());
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds() < 0 ? Integer.MAX_VALUE : properties.getAwaitTerminationSeconds());

        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorService executorServiceOfVirtual() {
        return Executors.newThreadPerTaskExecutor(
                srcRunnable -> Thread.ofVirtual().unstarted(mdcDecorator.apply(srcRunnable))
        );
    }

    @Bean
    public ScheduledExecutorService singleThreadScheduledExecutorService() {
        return Executors.newScheduledThreadPool(1,
                srcRunnable -> Thread.ofPlatform().unstarted(mdcDecorator.apply(srcRunnable))
        );
    }

    private final Function<Runnable, Runnable> mdcDecorator = srcRunnable -> {
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
