package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

@Getter
@Validated
@AllArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = OkhttpProperties.CONFIGURATION_PREFIX, ignoreUnknownFields = false)
public class OkhttpProperties {

    static final String CONFIGURATION_PREFIX = "http-client";

    /**
     *  Максимальное количество простаивающих (неактивных) HTTP-соединений,
     *  которые пул соединений будет поддерживать для каждого адреса
     */
    @PositiveOrZero
    private final int maxIdleConnections;

    /**
     * Определяет, как долго неактивное соединение может оставаться в пуле соединений перед закрытием
     */
    @Positive
    private final int keepAliveMillis;

    /**
     * Ограничивает общее количество параллельных запросов, которые
     * OkHttpClient может выполнять одновременно на любые http-адреса вместе
     */
    @Positive
    private final int maxParallelRequests;

    /**
     * Максимальное количество одновременных запросов, выполняемых к одному хосту (домену или IP-адресу), остальные
     * будут поставлены в очередь. При нахождении запросов в очереди - их {@code connectTimeout} расходуется
     */
    @Positive
    private final int maxParallelRequestsPerHost;

    /**
     * Максимальное время установления TCP-соединения с сервером. Если соединение не будет установлено в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private final int connectTimeout;

    /**
     * Максимальное время, ожидания данных от сервера. Если данные не будут получены в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private final int readTimeout;

    /**
     * Максимальное время, ожидания отправки данных на сервер. Если данные не будут отправлены в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private final int writeTimeout;

}
