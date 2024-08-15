package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(
        prefix = OkhttpProperties.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class OkhttpProperties {

    static final String CONFIGURATION_PREFIX = "http-client";

    /**
     *  Максимальное количество простаивающих (неактивных) HTTP-соединений,
     *  которые пул соединений будет поддерживать для каждого адреса
     */
    @PositiveOrZero
    private int maxIdleConnections;

    /**
     * Определяет, как долго неактивное соединение может оставаться в пуле соединений перед закрытием
     */
    @Positive
    private int keepAliveMillis;

    /**
     * Ограничивает общее количество параллельных запросов, которые
     * OkHttpClient может выполнять одновременно на любые http-адреса вместе
     */
    @Positive
    private int maxParallelRequests;

    /**
     * Максимальное количество одновременных запросов, выполняемых к одному хосту (домену или IP-адресу), остальные
     * будут поставлены в очередь. При нахождении запросов в очереди - их {@code connectTimeout} расходуется
     */
    @Positive
    private int maxParallelRequestsPerHost;

    /**
     * Максимальное время установления TCP-соединения с сервером. Если соединение не будет установлено в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private int connectTimeout;

    /**
     * Максимальное время, ожидания данных от сервера. Если данные не будут получены в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private int readTimeout;

    /**
     * Максимальное время, ожидания отправки данных на сервер. Если данные не будут отправлены в течение этого
     * времени - будет выброшено {@link SocketTimeoutException}. Исчисляется в {@link TimeUnit#MILLISECONDS}
     */
    @Positive
    private int writeTimeout;

}
