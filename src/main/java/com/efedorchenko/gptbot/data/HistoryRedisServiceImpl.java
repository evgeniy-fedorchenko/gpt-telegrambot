package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.configuration.properties.RedisProperties;
import com.efedorchenko.gptbot.yandex.model.GptMessageUnit;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Component
public class HistoryRedisServiceImpl implements HistoryRedisService {

    private final RedisTemplate<String, GptMessageUnit> redisTemplate;
    private final RedisProperties redisProperties;
    private final Duration messHistoryTtl;

    public HistoryRedisServiceImpl(RedisTemplate<String, GptMessageUnit> redisTemplate, RedisProperties redisProperties) {
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
        this.messHistoryTtl = Duration.of(redisProperties.getHistoryTtlMillis(), ChronoUnit.MILLIS);
    }


    @Override
    public @NotNull List<GptMessageUnit> getHistory(String userChatId) {
        String key = redisProperties.getHistoryPrefix() + userChatId;
        List<GptMessageUnit> gptMessageUnits = redisTemplate.opsForList().range(key, 0, -1);
        if (gptMessageUnits == null) {
            return Collections.emptyList();
        }
        return gptMessageUnits;
    }

    @Override
    public void addMessage(String userChatId, GptMessageUnit gptMessageUnit) {
        String key = redisProperties.getHistoryPrefix() + userChatId;
        redisTemplate.opsForList().rightPush(key, gptMessageUnit);
        redisTemplate.opsForList().trim(key, -redisProperties.getHistoryQueueCapacity(), -1);
        redisTemplate.expire(key, messHistoryTtl);
    }

    @Override
    public void clean(String chatId) {
        String key = redisProperties.getHistoryPrefix() + chatId;
        redisTemplate.delete(key);
    }

}
