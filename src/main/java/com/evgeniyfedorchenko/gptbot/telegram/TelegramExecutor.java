package com.evgeniyfedorchenko.gptbot.telegram;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Slf4j
@Component
public class TelegramExecutor extends DefaultAbsSender {

    protected TelegramExecutor(@Value("${telegram-bot.token}") String botToken) {
        super(new DefaultBotOptions(), botToken);
    }

    /**
     * Метод для непосредственной оправки сообщения на сервера Telegram. В случае ошибки отправки исключение
     * логируется как {@code TelegramApiException was thrown. Ex: ex} и подавляется
     *
     * @param method {@code @NotNull} Объект сообщения, готового к отправке
     * @return true, если сообщение было успешно отправлено, иначе false
     */
    public boolean send(PartialBotApiMethod<?> method) {

        boolean suc = true;

        try {
            switch (method) {
                case SendSticker sendSticker -> execute(sendSticker);
                case SendPhoto sendPhoto -> execute(sendPhoto);

                case SendMessage sendMessage -> {
                    sendMessage.enableMarkdown(true);
                    execute(sendMessage);
                }
                case EditMessageText editMessageText -> {
                    editMessageText.enableMarkdown(true);
                    execute(editMessageText);
                }
                case BotApiMethod<? extends Serializable> other -> execute(other);

                default -> {
                    log.error("Unexpected value to DefaultAbsSender.execute(): {}", method.getClass());
                    suc = false;
                }
            }
            return suc;
        } catch (TelegramApiException firstEx) {

//          fail. Maybe without markdown will send?
            try {
                if (method instanceof SendMessage mess) {
                    mess.enableMarkdownV2(false);
                    execute(mess);

                    log.debug("Markdown disable. Ex:{}", firstEx.getMessage());
                    return suc;
                }
                if (method instanceof EditMessageText edit) {
                    edit.enableMarkdown(false);
                    execute(edit);

                    log.debug("Markdown disable. Ex: {}", firstEx.getMessage());
                    return suc;
                }

//            epic fail
            } catch (TelegramApiException secondEx) {
                log.error("TelegramApiException was thrown. Cannot send text messages with markdown and without it. Mess: {}\n\nExes:\n\nFirst(with md): {}\n\nSecond(without md):", method, firstEx, secondEx);
                return false;
            }

            log.error("TelegramApiException was thrown. Cannot send this object: {}, ex:", method, firstEx);
            return false;
        }
    }

    /**
     * Метод для отправки сообщения и возвращаемым значением {@link Message}.
     * Возвращается {@code null}, если не удалось отправить сообщение. Это значит
     * что было выброшено исключение, которое было подавлено и залогировано
     * @param messToSend сообщение для отправки
     * @return успешно отправленное сообщение
     */
    public @Nullable Message sendAndReturn(SendMessage messToSend) {
        try {
            return execute(messToSend);
        } catch (TelegramApiException ex) {
            log.error("TelegramApiException was thrown. Cause: ", ex);
        }
        return null;
    }
}
