package com.evgeniyfedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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

    @Positive
    private int maxIdleConnections;
    @Positive
    private int keepAliveMillis;
    @Positive
    private int maxParallelRequests;
    @Positive
    private int maxParallelRequestsPerHost;
    @Positive
    private int connectTimeout;
    @Positive
    private int readTimeout;
    @Positive
    private int writeTimeout;

}
