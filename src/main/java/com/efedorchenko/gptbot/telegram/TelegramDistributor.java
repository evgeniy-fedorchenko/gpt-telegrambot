package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.aop.Log;
import com.efedorchenko.gptbot.configuration.properties.DefaultBotAnswer;
import com.efedorchenko.gptbot.data.HistoryRedisService;
import com.efedorchenko.gptbot.data.UserModeRedisService;
import com.efedorchenko.gptbot.service.TelegramService;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

import static com.efedorchenko.gptbot.telegram.TelegramDistributor.Command.*;

@Component
public class TelegramDistributor {

    /**
     * Карта для обработки команд бота
     * <lu>
     * <li><b>key</b> - команда бота в том виде, в каком ее отправляет юзер и соответственно ловит этот бот</li>
     * <li><b>value</b> - функция, возвращающая полностью готовый для отправки объект {@link SendMessage}</li>
     * </lu>
     */
    private final Map<String, Function<String, SendMessage>> commandReactMap;

    /**
     * Функция возвращающая готовый для отправки объекта {@link SendMessage} для ответа на неизвестную
     * команду. Используется как дефолтное значение карты {@link TelegramDistributor#commandReactMap}
     * в методе {@link Map#getOrDefault(Object, Object)}
     */
    private final Function<String, SendMessage> unsupportedCommandReact;

    private final UserModeRedisService userModeCache;
    private final TelegramService telegramService;
    private final DefaultBotAnswer defaultBotAnswer;

    public TelegramDistributor(UserModeRedisService userModeCache,
                               HistoryRedisService historyCache,
                               TelegramService telegramService,
                               DefaultBotAnswer defaultBotAnswer) {
        this.userModeCache = userModeCache;
        this.telegramService = telegramService;
        this.defaultBotAnswer = defaultBotAnswer;

        this.unsupportedCommandReact = chatId -> new SendMessage(chatId, defaultBotAnswer.unknown());
        this.commandReactMap = Map.of(
                START.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.start()),
                HELP.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.help()),
                FEEDBACK.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.feedback()),

                YA_GPT.getRepresentation(), chatId -> {
                    userModeCache.setMode(chatId, Mode.YANDEX_GPT);
                    historyCache.clean(chatId);
                    return new SendMessage(chatId, defaultBotAnswer.yagpt());
                },
                YA_ART.getRepresentation(), chatId -> {
                    userModeCache.setMode(chatId, Mode.YANDEX_ART);
                    return new SendMessage(chatId, defaultBotAnswer.yaart());
                }
        );
    }

    /**
     * Метод принимает входящий {@code update} и распределяет его по обработчикам в зависимости от содержания
     * Возвращается {@code null}, если в процессе обработки становится понятно, что на это действие
     * не нужно ничего отвечать. (Например, если пришло уведомление о закрепленном сообщении)
     *
     * @param update объект, представляющий уведомление о каком-то действии в чате
     * @return готовый объект для возврата юзеру или {@code null}, если не нужно ничего возвращать
     */
    @Log
    public @Nullable PartialBotApiMethod<? extends Serializable> distribute(Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());

//        На закрепление сообщения ничего не отвечаем
        if (inMess.getPinnedMessage() != null) {
            return null;
        }

//        Если нет текста и ГС - даем ОС (подпись под фото не считается за текст)
        if (!inMess.hasText() && !inMess.hasVoice()) {
            return new SendMessage(String.valueOf(chatId), defaultBotAnswer.invalidDataFormat());
        }

//        Block if user wait for the image as YANDEX_ART mode
        Mode currentMode = userModeCache.getMode(chatId); // Для аварийного сброса: userModeCache.setMode(chatId, Mode.YANDEX_ART)
        if (currentMode.equals(Mode.YANDEX_ART_HOLD)) {
            return new SendMessage(chatId, defaultBotAnswer.artGenProcessing());
        }

//        Handle commands
        if (inMess.isCommand()) {
            return commandReactMap.getOrDefault(inMess.getText(), unsupportedCommandReact).apply(chatId);
        }

//        Main processing
        return telegramService.processing(currentMode, update);
    }

    @Getter
    @AllArgsConstructor
    public enum Command {

        START("/start"),
        HELP("/help"),
        FEEDBACK("/feedback"),
        YA_GPT("/gpt"),
        YA_ART("/art");

        private final String representation;

    }
}
