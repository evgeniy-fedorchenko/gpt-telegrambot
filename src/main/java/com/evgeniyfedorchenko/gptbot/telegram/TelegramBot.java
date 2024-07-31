package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.yandex.YandexCaller;
import com.evgeniyfedorchenko.gptbot.yandex.YandexService;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final YandexService yandexService;

    public TelegramBot(@Value("${telegram-bot.token}") String botToken,
                       YandexService yandexService) {
        super(botToken);
        this.yandexService = yandexService;
    }

    @Override
    public String getBotUsername() {
        return "gpt_exp_bot";
    }

    /**
     * Метод получения сообщений непосредственно с серверов Telegram, а так же их маршрутизации
     * по методам обработки. Полученный результат отправляется обратно на сервера Telegram с
     * помощью метода {@link  TelegramBot#send(SendMessage)}
     *
     * @param update корневой объект, содержащий всю информацию о пришедшем обновлении
     */
    @Override
    public void onUpdateReceived(Update update) {

        if (update != null && update.hasMessage()) {

            log.debug("Processing has BEGUN for updateID {}", update.getUpdateId());

            CompletableFuture.supplyAsync(() -> processing(update))
                    .thenAccept(this::send)

                    .exceptionally(ex -> {
                        log.error("ex: ", ex);
                        return null;
                    });

            log.debug("Processing has ENDED for updateID {}", update.getUpdateId());
        }
    }

    private SendMessage processing(Update update) {

        Message inMess = update.getMessage();

        String answerText = yandexService.newCall(inMess);
        String chatId = String.valueOf(inMess.getChatId());

        return new SendMessage(chatId, answerText);
    }

    private void send(SendMessage messToSend) {
        try {
            this.executeAsync(messToSend);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
            // TODO 31.07.2024 17:14
        }
    }
}


