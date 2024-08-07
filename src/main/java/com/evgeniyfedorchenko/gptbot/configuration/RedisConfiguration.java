package com.evgeniyfedorchenko.gptbot.configuration;

import com.evgeniyfedorchenko.gptbot.telegram.Mode;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@AllArgsConstructor
public class RedisConfiguration {

    private final ObjectMapper objectMapper;

    /**
     * Кеш для хранения истории сообщений в режиме YandexGPT<br>
     * <lu>
     * <li>{@code key} chatId юзера</li>
     * <li>{@code value} объект {@code com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit.java}
     * как единица сообщения в истории сообщений (должна храниться в списке таким сообщений)</li>
     * </lu>
     */
    @Bean
    public RedisTemplate<String, GptMessageUnit> historyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GptMessageUnit> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<GptMessageUnit> messSerializer
                = new Jackson2JsonRedisSerializer<>(objectMapper, GptMessageUnit.class);


        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(messSerializer);

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Mode> userModeRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Mode> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<Mode> modeSerializer
                = new Jackson2JsonRedisSerializer<>(objectMapper, Mode.class);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(modeSerializer);

        return redisTemplate;
    }
}
