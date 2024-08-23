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
            String key = redisProperties.getUserModePrefix() + userChatId;
            redisTemplate.opsForValue().set(key, mode);
            redisTemplate.expire(key, Duration.of(redisProperties.getUserModeTtlMillis(), ChronoUnit.MILLIS));
        }
    }

    /**
     * Метод сбрасывает режимы у юзеров, находящихся в режиме {@link Mode#YANDEX_ART_HOLD} при старте приложения
     * <p>
     * При остановке сервера (неважно - плановое завершение работы или резкая остановка) может случиться
     * ситуация, что юзеры, которые находились в процессе генерации изображения прямо в момент остановки
     * сервера, так и останутся в этом режиме и при запуске сервера не смогут ничего сделать, тк из-за этого
     * режима {@link TelegramDistributor#distribute(Update)} их не пустит дальше и будет ждать сброса режима,
     * который никогда не произойдет.
     * Так как при остановке сервера через {@code @PreDestroy} сбрасывать режимы не совсем
     * безопасно, потому что при резкой остановке процесса этот метод не успеет выполниться.
     * Лучше сделать проверку на статусы {@link Mode#YANDEX_ART_HOLD} перед запуском
     */
    @PostConstruct
    public void disableYandexArtHold() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            keys.forEach(key -> redisTemplate.opsForValue().set(key, Mode.YANDEX_ART));
        }
    }
}
