package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.telegram.Mode;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserModeRedisServiceImpl implements UserModeRedisService {

    private final RedisTemplate<String, Mode> redisTemplate;
    private static final String PREFIX = "mode-";


    @Override
    public Mode getMode(String userChatId) {

        Mode mode = redisTemplate.opsForValue().get(PREFIX + userChatId);
        if (mode == null) {
            redisTemplate.opsForValue().set(PREFIX + userChatId, Mode.YANDEX_GPT);
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
