package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.data.RedisService;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptMessageUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@AllArgsConstructor
public class YandexService {

    private final RedisService redisService;
    private final YandexCaller yandexCaller;

    public String newCall(Message telegramMess) {

        String userChatId = String.valueOf(telegramMess.getChatId());
        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), telegramMess.getText());

        List<GptMessageUnit> history = redisService.getHistory(userChatId);
        history.add(question);

        GptMessageUnit answer = yandexCaller.buildRequest(history)
                .result()
                .alternatives()
                .get(0)
                .gptMessageUnit();

        CompletableFuture.runAsync(() -> {
            redisService.addMessage(userChatId, question);
            redisService.addMessage(userChatId, answer);
        });
        return answer.text();
    }
}
