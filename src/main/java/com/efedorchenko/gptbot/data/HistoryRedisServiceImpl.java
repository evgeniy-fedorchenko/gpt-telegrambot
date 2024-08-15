package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.yandex.model.GptMessageUnit;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class HistoryRedisServiceImpl implements HistoryRedisService {

    private final RedisTemplate<String, GptMessageUnit> redisTemplate;
    private static final int MAX_MESSAGES = 30;
    private static final Duration MESS_HISTORY_TTL = Duration.of(2, ChronoUnit.HOURS);

    private static final String PREFIX = "history-";

    @Override
    public @NotNull List<GptMessageUnit> getHistory(String userChatId) {
        List<GptMessageUnit> gptMessageUnits = redisTemplate.opsForList().range(PREFIX + userChatId, 0, -1);
        if (gptMessageUnits == null) {
            return Collections.emptyList();
        }
        return gptMessageUnits;
    }

    @Override
    public void addMessage(String userChatId, GptMessageUnit gptMessageUnit) {
        redisTemplate.opsForList().rightPush(PREFIX + userChatId, gptMessageUnit);
        redisTemplate.opsForList().trim(PREFIX + userChatId, -MAX_MESSAGES, -1);
        redisTemplate.expire(PREFIX + userChatId, MESS_HISTORY_TTL);
    }

    @Override
    public void clean(String chatId) {
        redisTemplate.delete(PREFIX + chatId);
    }

}
