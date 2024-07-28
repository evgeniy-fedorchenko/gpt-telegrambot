package com.evgeniyfedorchenko.gptbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    public TelegramBot(@Value("${telegram-bot.token}") String botToken) {
        super(botToken);
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

        if (update != null) {

            log.debug("Processing has BEGUN for updateID {}", update.getUpdateId());

            SendMessage messToSend = processing(update);
            this.send(messToSend);

            log.debug("Processing has ENDED for updateID {}", update.getUpdateId());
        }
    }

    private SendMessage processing(Update update) {
        return new SendMessage(String.valueOf(update.getMessage().getChatId()), "I hear!");
    }

    private void send(SendMessage messToSend) {
        try {
            this.execute(messToSend);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}


