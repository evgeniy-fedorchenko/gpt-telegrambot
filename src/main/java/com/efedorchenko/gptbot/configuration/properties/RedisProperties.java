package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    /**
     * Regexp: Строка длиной от 2 до 15 всего (обе границы вкл). Разрешены любые символы,
     * кроме пробельных, должна содержать хотя бы одну латинскую букву и заканчиваться на
     * знак минуса (символ "Modifier Letter Minus Sign", hex UTF-8: U+02D7)
     */
    private static final String PREFIX_REGEX = "^(?=.{2,15}$)(?=.*[a-zA-Z])\\S+-$";

//     Свойства для кеша RedisConfiguration.historyRedisTemplate
    @NotBlank
    @Pattern(regexp = PREFIX_REGEX)
    private final String historyPrefix;
    @Positive
    private final int historyTtlMillis;
    @Positive
    private final int historyQueueCapacity;

//     Свойства для кеша RedisConfiguration.userModeRedisTemplate
    @NotBlank
    @Pattern(regexp = PREFIX_REGEX)
    private final String userModePrefix;
    @Positive
    private final int userModeTtlMillis;

//     Свойства для менеджера кеша RedisConfiguration.userIsSubscribedRedisCacheManager
    @Positive
    private final int userIsSubTtlMillis;

}
