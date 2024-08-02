package com.evgeniyfedorchenko.gptbot.configuration;

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

    @Bean
    public RedisTemplate<String, GptMessageUnit> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GptMessageUnit> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<GptMessageUnit> messSerializer
                = new Jackson2JsonRedisSerializer<>(objectMapper, GptMessageUnit.class);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(messSerializer);

        return redisTemplate;
    }
}
