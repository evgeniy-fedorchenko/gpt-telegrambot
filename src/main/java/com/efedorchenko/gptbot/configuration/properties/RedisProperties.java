package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@AllArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = RedisProperties.CONFIGURATION_PREFIX, ignoreUnknownFields = false)
public class RedisProperties {

    static final String CONFIGURATION_PREFIX = "redis";

    @NotBlank
    private final String historyPrefix;

    @Positive
    private final int historyTtlMillis;

    @Positive
    private final int historyQueueCapacity;

    @NotBlank
    private final String userModePrefix;

    @Positive
    private final int userModeTtlMillis;

}
