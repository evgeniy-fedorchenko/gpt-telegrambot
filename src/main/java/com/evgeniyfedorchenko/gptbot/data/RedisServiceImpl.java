package com.evgeniyfedorchenko.gptbot.data;

import com.evgeniyfedorchenko.gptbot.yandex.models.GptMessageUnit;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, GptMessageUnit> redisTemplate;
    private static final int MAX_MESSAGES = 100;
    private static final Duration MESS_HISTORY_TTL = Duration.of(2, ChronoUnit.HOURS);

    @Override
    public List<GptMessageUnit> getHistory(String userChatId) {
        List<GptMessageUnit> gptMessageUnits = redisTemplate.opsForList().range(userChatId, 0, -1);
        if (gptMessageUnits == null) {
            return Collections.emptyList();
        }
        return gptMessageUnits;
    }

    @Override
    public void addMessage(String userChatId, GptMessageUnit gptMessageUnit) {
        redisTemplate.opsForList().rightPush(userChatId, gptMessageUnit);
        redisTemplate.opsForList().trim(userChatId, -MAX_MESSAGES, -1);
        redisTemplate.expire(userChatId, MESS_HISTORY_TTL);
    }

    @Override
    public Boolean exist(String userChatId) {
        return redisTemplate.hasKey(userChatId);
    }
}
