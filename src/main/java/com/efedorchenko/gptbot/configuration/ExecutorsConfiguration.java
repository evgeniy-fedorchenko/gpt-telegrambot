package com.efedorchenko.gptbot.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@Setter
@Validated
@EnableAsync
@Configuration
@ConfigurationProperties(
        prefix = ExecutorsConfiguration.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class ExecutorsConfiguration {

    static final String CONFIGURATION_PREFIX = "executor";
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * Множитель минимального числа потоков в пуле для получения максимального числа потоков.
     * Определяется как {@code int MaxPoolSize = poolSizeMultiplierForMaxPoolSize * POOL_SIZE},
     * где {@code POOL_SIZE} задается в системе как {@code Runtime.getRuntime().availableProcessors()}
     */
    @Positive
    private int poolSizeMultiplierForMaxPoolSize;

    /**
     * Максимальное количество задач, которые могут находиться в очереди исполнителя.
     * При переполнении очереди будет выброшено {@link  RejectedExecutionException}
     * <lu>
     *     <li>Отрицательное означает, что очередь ограничена {@link Integer#MAX_VALUE}</li>
     *     <li>Ноль означает нулевой размер очереди</li>
     * </lu>
     */
    private int queueCapacity;

    /**
     * Время в секундах, в течение которого неактивные потоки будут оставаться живыми,
     * прежде чем будут завершены
     * <lu>
     *     <li>Отрицательное значение означает, что потоки жить {@link Integer#MAX_VALUE} секунд</li>
     *     <li>Ноль означает, что неактивные потоки будут завершаться немедленно</li>
     * </lu>
     */
    private int keepAliveSeconds;

    /**
     * Время в секундах, которое исполнитель будет ждать завершения всех задач
     * после вызова метода {@code shutdown()}.
     * <lu>
     *     <li>Отрицательное значение означает, что потоки смогут завершаться {@link Integer#MAX_VALUE} секунд</li>
     *     <li>Ноль означает, что потоки будут немедленно прерваны</li>
     * </lu>
     */
    private int awaitTerminationSeconds;

    /**
     * Префикс, который будет использоваться для имен потоков, создаваемых исполнителем.
     * Не может быть пустым, макс. длина - 15 символов
     */
    @NotEmpty
    @Size(max = 15)
    private String threadNamePrefix;

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(poolSizeMultiplierForMaxPoolSize * POOL_SIZE);
        executor.setQueueCapacity(queueCapacity < 0 ? Integer.MAX_VALUE : queueCapacity);

        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setKeepAliveSeconds(keepAliveSeconds < 0 ? Integer.MAX_VALUE : keepAliveSeconds);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds < 0 ? Integer.MAX_VALUE : awaitTerminationSeconds);

        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorService executorServiceOfVirtual(VirtualThreadFactory threadFactory) {
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @Bean
    public ScheduledExecutorService singleThreadScheduledExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }
}
