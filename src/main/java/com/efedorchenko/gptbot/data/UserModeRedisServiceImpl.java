package com.efedorchenko.gptbot.data;

import com.efedorchenko.gptbot.configuration.properties.RedisProperties;
import com.efedorchenko.gptbot.telegram.Mode;
import com.efedorchenko.gptbot.telegram.TelegramDistributor;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@AllArgsConstructor
public class UserModeRedisServiceImpl implements UserModeRedisService {

    private final RedisTemplate<String, String> userModeRedisTemplate;
    private final RedisProperties redisProperties;

    @Override
    public Mode getMode(String userChatId) {
        String key = redisProperties.getUserModePrefix() + userChatId;
        String modeStr = userModeRedisTemplate.opsForValue().get(key);

        if (modeStr == null) {
            setMode(userChatId, Mode.YANDEX_GPT);
            return Mode.YANDEX_GPT;
        }
        return Mode.valueOf(modeStr);
    }

    @Override
    public void setMode(String userChatId, Mode mode) {
        if (mode != null) {
            String modeStr = mode.name();
            String key = redisProperties.getUserModePrefix() + userChatId;

            userModeRedisTemplate.opsForValue().set(key, modeStr);
            userModeRedisTemplate.expire(key, Duration.of(redisProperties.getUserModeTtlMillis(), ChronoUnit.MILLIS));
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
        Set<String> keys = userModeRedisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            keys.stream()
                    .filter(key -> key.startsWith(redisProperties.getUserModePrefix()))
                    .filter(key -> Objects.equals(userModeRedisTemplate.opsForValue().get(key), Mode.YANDEX_ART_HOLD.name()))
                    .forEach(key -> userModeRedisTemplate.opsForValue().set(key, Mode.YANDEX_ART.name()));
        }
    }
}
