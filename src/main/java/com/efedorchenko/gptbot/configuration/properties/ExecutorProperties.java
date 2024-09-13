package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.RejectedExecutionException;

@Getter
@Validated
@AllArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = ExecutorProperties.CONFIGURATION_PREFIX, ignoreUnknownFields = false)
public class ExecutorProperties {

    static final String CONFIGURATION_PREFIX = "executor";
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * Множитель минимального числа потоков в пуле для получения максимального числа потоков.
     * Определяется как {@code int MaxPoolSize = poolSizeMultiplierForMaxPoolSize * POOL_SIZE},
     * где {@code POOL_SIZE} задается в системе как {@code Runtime.getRuntime().availableProcessors()}
     */
    @Positive
    private final int poolSizeMultiplierForMaxPoolSize;

    /**
     * Максимальное количество задач, которые могут находиться в очереди исполнителя.
     * При переполнении очереди будет выброшено {@link  RejectedExecutionException}
     * <lu>
     *     <li>Отрицательное означает, что очередь ограничена {@link Integer#MAX_VALUE}</li>
     *     <li>Ноль означает нулевой размер очереди</li>
     * </lu>
     */
    private final int queueCapacity;

    /**
     * Время в секундах, в течение которого неактивные потоки будут оставаться живыми,
     * прежде чем будут завершены
     * <lu>
     *     <li>Отрицательное значение означает, что потоки жить {@link Integer#MAX_VALUE} секунд</li>
     *     <li>Ноль означает, что неактивные потоки будут завершаться немедленно</li>
     * </lu>
     */
    private final int keepAliveSeconds;

    /**
     * Время в секундах, которое исполнитель будет ждать завершения всех задач
     * после вызова метода {@code shutdown()}.
     * <lu>
     *     <li>Отрицательное значение означает, что потоки смогут завершаться {@link Integer#MAX_VALUE} секунд</li>
     *     <li>Ноль означает, что потоки будут немедленно прерваны</li>
     * </lu>
     */
    private final int awaitTerminationSeconds;

    /**
     * Префикс, который будет использоваться для имен потоков, создаваемых исполнителем.
     * Не может быть пустым, макс. длина - 15 символов
     */
    @NotEmpty
    @Size(max = 15)
    private final String threadNamePrefix;

}
