package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.yandex.YandexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.io.Serializable;
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

    private boolean send(PartialBotApiMethod<? extends Serializable> method) {
        boolean suc = true;
        try {
            switch (method) {
                case SendSticker sendSticker -> execute(sendSticker);
                case SendPhoto sendPhoto -> execute(sendPhoto);
                case BotApiMethod<? extends Serializable> other -> execute(other);
                default -> {
                    log.error("Unexpected value to DefaultAbsSender.execute(): {}", method.getClass());
                    suc = false;
                }
            }
            return suc;
        } catch (TelegramApiException ex) {
            log.error("TelegramApiException was thrown. Cause: {}", ex.getMessage());
            return false;
        }


//        try {
//            this.execute(messToSend);
//        } catch (TelegramApiException e) {
//            messToSend.enableMarkdown(false);
//            log.warn("Markdown disable");
//            try {
//                this.execute(messToSend);
//            } catch (TelegramApiException ex) {
//                throw new RuntimeException(ex);
//            }
            // TODO 31.07.2024 17:14
        }
    }



