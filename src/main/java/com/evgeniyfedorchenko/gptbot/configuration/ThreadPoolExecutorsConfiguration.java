package com.evgeniyfedorchenko.gptbot.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
@Validated
@EnableAsync
@Configuration
@ConfigurationProperties(
        prefix = ThreadPoolExecutorsConfiguration.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class ThreadPoolExecutorsConfiguration {

    static final String CONFIGURATION_PREFIX = "executor";
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    @Positive
    private int poolSizeMultiplierForMaxPoolSize;
    @PositiveOrZero
    private int queueCapacity;
    @PositiveOrZero
    private int keepAliveSeconds;
    @PositiveOrZero
    private int awaitTerminationSeconds;
    @NotEmpty
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

    @Bean
    public ExecutorService executorServiceOfVirtual() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

}
