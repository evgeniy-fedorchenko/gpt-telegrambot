package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.yandex.YandexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final YandexService yandexService;
    private final TelegramExecutor telegramExecutor;

    public TelegramBot(@Value("${telegram-bot.token}") String botToken,
                       YandexService yandexService,
                       TelegramExecutor telegramExecutor) {
        super(botToken);
        this.yandexService = yandexService;
        this.telegramExecutor = telegramExecutor;
    }

    @Override
    public String getBotUsername() {
        return "gpt_exp_bot";
    }

    /**
     * Точка входа в приложение со стороны Телеграм-бота
     * Метод первично валидирует принятый объект, после чего направляет на маршрутизацию и обработку.
     * После обработки ответ направляется в {@link TelegramExecutor#send(PartialBotApiMethod)} для
     * отправки контента юзеру
     *
     * @param update корневой объект, содержащий всю информацию о пришедшем обновлении
     */
    @Override
    public void onUpdateReceived(Update update) {

        if (update != null && update.hasMessage()) {

            log.debug("Processing has BEGUN for updateID {}", update.getUpdateId());

            CompletableFuture.supplyAsync(() -> processing(update))
                    .thenAccept(telegramExecutor::send)

                    .exceptionally(ex -> {
                        log.error("ex: ", ex.getCause());
                        return null;
                    });

            log.debug("Processing has ENDED for updateID {}", update.getUpdateId());
        }
    }

    private PartialBotApiMethod<? extends Serializable> processing(Update update) {

        Message inMess = update.getMessage();

        if (inMess.getText().startsWith("check ")) {
            InputStream is = yandexService.getReadyPicture(inMess.getText().replace("check ", ""));
            InputFile result = new InputFile(is, "result");
            return new SendPhoto(String.valueOf(inMess.getChatId()), result);
        }
        String imageId = yandexService.newPicture(update.getMessage());
        return new SendMessage(String.valueOf(update.getMessage().getChatId()), imageId);
//        String answerText = yandexService.newCall(inMess);
//        String chatId = String.valueOf(inMess.getChatId());
//
//        SendMessage sendMessage = new SendMessage();
//        sendMessage.enableMarkdown(true);
//        sendMessage.setChatId(chatId);
//        sendMessage.setText(answerText);

//        return sendMessage;
    }
}
