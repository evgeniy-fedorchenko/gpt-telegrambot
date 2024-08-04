package com.evgeniyfedorchenko.gptbot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Getter
@Setter
@Configuration
@ConfigurationProperties(
        prefix = ThreadPoolTaskExecutorConfiguration.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class ThreadPoolTaskExecutorConfiguration {

    static final String CONFIGURATION_PREFIX = "executor";
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private int poolSizeMultiplierForMaxPoolSize;
    private int queueCapacity;
    private int keepAliveSeconds;
    private int awaitTerminationSeconds;
    private String threadNamePrefix;

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(poolSizeMultiplierForMaxPoolSize * POOL_SIZE);
        executor.setQueueCapacity(queueCapacity);

        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        executor.initialize();
        return executor;
    }

}
