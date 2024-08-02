package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.data.RedisService;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@AllArgsConstructor
public class YandexService {

    private final RedisService redisService;
    private final YandexCaller yandexCaller;

    public String newCall(Message telegramMess) {

        String userChatId = String.valueOf(telegramMess.getChatId());
        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), telegramMess.getText());
        log.info("USER: {}", telegramMess.getText());

        List<GptMessageUnit> history = redisService.getHistory(userChatId);
        history.add(question);

        GptMessageUnit answer = yandexCaller.buildRequest(history)
                .result()
                .alternatives()
                .getFirst()
                .message();

        CompletableFuture.runAsync(() -> {
            redisService.addMessage(userChatId, question);
            redisService.addMessage(userChatId, answer);
        });
        log.info("ASSISTANT: {}", answer.text());
        return answer.text();
    }

    public String newPicture(Message telegramMess) {
        return yandexCaller.buildRequestArt(telegramMess.getText());
    }

    public InputStream getReadyPicture(String imageId) {
        String imageBase64 = yandexCaller.checkImage(imageId);

        byte[] bytes = Base64.getDecoder().decode(imageBase64);
         return new ByteArrayInputStream(bytes);

    }
}
