package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.configuration.RedisConfiguration;
import com.efedorchenko.gptbot.configuration.properties.TelegramProperties;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import static com.efedorchenko.gptbot.utils.logging.LogUtils.FUTURE_CHECK;
import static com.efedorchenko.gptbot.utils.logging.LogUtils.LOGIC_MARKER;

@Slf4j
@Component
public class TelegramExecutor extends DefaultAbsSender {

    private static final String RIGHT_DEFAULT_STATUS = "member";

    protected TelegramExecutor(TelegramProperties telegramProperties) {
        super(new DefaultBotOptions(), telegramProperties.getToken());
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
                case SendVoice sendVoice -> execute(sendVoice);

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
                    log.error(LOGIC_MARKER, "Unexpected value to DefaultAbsSender.execute(): {}", method.getClass());
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

                    log.warn(LOGIC_MARKER, "Markdown disable. Ex:{}", firstEx.getMessage());
                    return true;
                }
                if (method instanceof EditMessageText edit) {
                    edit.enableMarkdown(false);
                    execute(edit);

                    log.warn(LOGIC_MARKER, "Markdown disable. Ex: {}", firstEx.getMessage());
                    return true;
                }

//            epic fail
            } catch (TelegramApiException secondEx) {
                log.error(LOGIC_MARKER, "TelegramApiException was thrown. Cannot send text messages with markdown and without it. Mess: {}\n\nExes:\n\nFirst(with md): {}\n\nSecond(without md):", method, firstEx, secondEx);
                return false;
            }

            log.error(LOGIC_MARKER, "TelegramApiException was thrown. Cannot send this object: {}, ex:", method, firstEx);
            return false;
        }
    }

    /**
     * Метод для отправки сообщения и возвращаемым значением {@link Message}.
     * Возвращается {@code null}, если не удалось отправить сообщение. Это значит
     * что было выброшено исключение, которое было подавлено и залогировано
     *
     * @param messToSend сообщение для отправки
     * @return успешно отправленное сообщение
     */
    @Nullable
    public Message sendAndReturn(SendMessage messToSend) {
        try {
            return execute(messToSend);
        } catch (TelegramApiException ex) {
            log.error(LOGIC_MARKER, "TelegramApiException was thrown. Cause: ", ex);
        }
        return null;
    }

    public byte[] downloadVoice(Voice voice) {

        try (InputStream is = downloadFileAsStream(execute(new GetFile(voice.getFileId())))) {
            return is.readAllBytes();

        } catch (TelegramApiException | IOException ex) {
            log.error(LOGIC_MARKER, "{} was thrown. Cause: {}", ex.getClass().getSimpleName(), ex.getMessage());
            return new byte[0];
        }
    }

    @Cacheable(cacheNames = RedisConfiguration.USER_IS_SUB_CACHE_NAME, key = "#chatId")
    public String checkSubscribesPositive(long chatId, String channelLink) {

        try {
            return getChatMember(chatId, channelLink).getStatus();

        } catch (TelegramApiRequestException tare) {
            log.error(FUTURE_CHECK, "TelegramApiRequestException was thrown because bot is not admin of the channel '%s' to which the subscription is checked. Skipped, access to the bot is open. Cause: {}"
                    .formatted(channelLink), tare.getMessage());
        } catch (TelegramApiException tae) {
            log.error(LOGIC_MARKER, "TelegramApiException was thrown. Ex: ", tae);
        } catch (Exception ex) {
            log.error(LOGIC_MARKER, "Unknown exception was thrown. Skipped, access to the bot is open. Ex: ", ex);
        }

//        If you can't check status then return "member"
        return RIGHT_DEFAULT_STATUS;

    }

    private ChatMember getChatMember(long chatId, String s) throws TelegramApiException {
        return execute(new GetChatMember(s, chatId));
    }
}
