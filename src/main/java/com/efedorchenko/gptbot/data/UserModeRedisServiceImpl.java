package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.configuration.properties.RedisProperties;
import com.efedorchenko.gptbot.telegram.Mode;
import com.efedorchenko.gptbot.telegram.TelegramDistributor;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Component
@AllArgsConstructor
public class UserModeRedisServiceImpl implements UserModeRedisService {

    private final RedisTemplate<String, Mode> redisTemplate;
    private final RedisProperties redisProperties;

    @Override
    public Mode getMode(String userChatId) {

        Mode mode = redisTemplate.opsForValue().get(redisProperties.getUserModePrefix() + userChatId);
        if (mode == null) {
            setMode(userChatId, Mode.YANDEX_GPT);
            return Mode.YANDEX_GPT;
        }
        return mode;
    }

    @Override
    public void setMode(String userChatId, Mode mode) {
        if (mode != null) {
            redisTemplate.opsForValue().set(PREFIX + userChatId, mode);
        }
    }
}
