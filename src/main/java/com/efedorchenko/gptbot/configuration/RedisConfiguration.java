package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.configuration.properties.RedisProperties;
import com.efedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
@EnableCaching
@AllArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfiguration {
    public static final String USER_IS_SUB_CACHE_NAME = "sub";

    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;
    private final RedisConnectionFactory connectionFactory;



    /**
     * Кеш для хранения истории сообщений в режиме YandexGPT<br>
     * <lu>
     * <li>{@code key} chatId юзера</li>
     * <li>{@code value} объект {@link  GptMessageUnit}
     * как единица сообщения в истории сообщений (должна храниться в списке таких сообщений)</li>
     * </lu>
     */
    @Bean
    public RedisTemplate<String, GptMessageUnit> historyRedisTemplate() {
        RedisTemplate<String, GptMessageUnit> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<GptMessageUnit> messSerializer
                = new Jackson2JsonRedisSerializer<>(objectMapper, GptMessageUnit.class);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(messSerializer);

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, String> userModeRedisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public RedisCacheManager userIsSubscribedRedisCacheManager() {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()

                .serializeKeysWith(fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(fromSerializer(new StringRedisSerializer()))
                .entryTtl(Duration.ofMillis(redisProperties.getUserIsSubTtlMillis()))
                .disableKeyPrefix()   // Used cacheName as prefix
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}
