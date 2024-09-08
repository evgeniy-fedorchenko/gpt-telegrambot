package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.configuration.properties.RedisProperties;
import com.efedorchenko.gptbot.yandex.model.GptMessageUnit;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class HistoryRedisServiceImpl implements HistoryRedisService {

    private final RedisTemplate<String, GptMessageUnit> historyRedisTemplate;
    private final RedisProperties redisProperties;
    private final Duration messHistoryTtl;

    public HistoryRedisServiceImpl(RedisTemplate<String, GptMessageUnit> historyRedisTemplate, RedisProperties redisProperties) {
        this.historyRedisTemplate = historyRedisTemplate;
        this.redisProperties = redisProperties;
        this.messHistoryTtl = Duration.of(redisProperties.getHistoryTtlMillis(), ChronoUnit.MILLIS);
    }

    @NotNull
    @Override
    public List<GptMessageUnit> getHistory(String userChatId) {
        String key = redisProperties.getHistoryPrefix() + userChatId;
        List<GptMessageUnit> gptMessageUnits = historyRedisTemplate.opsForList().range(key, 0, -1);
        if (gptMessageUnits == null) {
            return Collections.emptyList();
        }
        return gptMessageUnits;
    }

    @Override
    public void addMessage(String userChatId, GptMessageUnit gptMessageUnit) {
        String key = redisProperties.getHistoryPrefix() + userChatId;
        historyRedisTemplate.opsForList().rightPush(key, gptMessageUnit);
        historyRedisTemplate.opsForList().trim(key, -redisProperties.getHistoryQueueCapacity(), -1);
        historyRedisTemplate.expire(key, messHistoryTtl);
    }

    @Override
    public void clean(String chatId) {
        String key = redisProperties.getHistoryPrefix() + chatId;
        historyRedisTemplate.delete(key);
    }

}
