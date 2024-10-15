package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.configuration.properties.DefaultBotAnswer;
import com.efedorchenko.gptbot.configuration.properties.TelegramProperties;
import com.efedorchenko.gptbot.data.HistoryRedisService;
import com.efedorchenko.gptbot.data.UserModeRedisService;
import com.efedorchenko.gptbot.service.TelegramService;
import com.efedorchenko.gptbot.utils.logging.Log;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.efedorchenko.gptbot.telegram.TelegramDistributor.Command.*;

@Slf4j
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

    private final TelegramService telegramService;
    private final TelegramExecutor telegramExecutor;
    private final DefaultBotAnswer defaultBotAnswer;
    private final UserModeRedisService userModeCache;
    private final TelegramProperties telegramProperties;

     public static final Set<String> rightStatuses = Set.of("creator", "administrator", "member", "restricted");


    public TelegramDistributor(UserModeRedisService userModeCache,
                               HistoryRedisService historyCache,
                               TelegramService telegramService,
                               DefaultBotAnswer defaultBotAnswer,
                               TelegramExecutor telegramExecutor,
                               TelegramProperties telegramProperties) {
        this.userModeCache = userModeCache;
        this.telegramService = telegramService;
        this.defaultBotAnswer = defaultBotAnswer;
        this.telegramProperties = telegramProperties;
        this.telegramExecutor = telegramExecutor;

        this.unsupportedCommandReact = chatId -> new SendMessage(chatId, defaultBotAnswer.unknownCommand());
        this.commandReactMap = Map.of(
                START.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.startCommand()),
                HELP.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.helpCommand()),
                FEEDBACK.getRepresentation(), chatId -> new SendMessage(chatId, defaultBotAnswer.feedbackCommand()),

                YA_GPT.getRepresentation(), chatId -> {
                    userModeCache.setMode(chatId, Mode.YANDEX_GPT);
                    historyCache.clean(chatId);
                    return new SendMessage(chatId, defaultBotAnswer.yagptCommand());
                },
                YA_ART.getRepresentation(), chatId -> {
                    userModeCache.setMode(chatId, Mode.YANDEX_ART);
                    return new SendMessage(chatId, defaultBotAnswer.yaartCommand());
                }
        );
    }

    /**
     * Метод принимает входящий {@code update} и распределяет его по обработчикам в зависимости от содержания.
     * Возвращается {@code null}, если в процессе обработки становится понятно, что на это действие
     * не нужно ничего отвечать. (Например, если пришло уведомление о закрепленном сообщении)
     *
     * @param update объект, представляющий уведомление о каком-то действии в чате
     * @return готовый объект для возврата юзеру или {@code null}, если не нужно ничего возвращать
     */
    @Log
    @Nullable
    public PartialBotApiMethod<? extends Serializable> distribute(Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());

//        Проверка подписок на каналы
//        CompletableFuture<Boolean> isSubscribeFuture =
//                CompletableFuture.supplyAsync(() -> checkSubscribes(inMess.getChatId()));

//        На закрепление сообщения ничего не делаем
        if (inMess.getPinnedMessage() != null) {
            return null;
        }

//        Если нет текста и ГС - даем ОС (подпись под фото не считается за текст)
        if (!inMess.hasText() && !inMess.hasVoice()) {
            return new SendMessage(chatId, defaultBotAnswer.invalidDataFormat());
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
//        PartialBotApiMethod<? extends Serializable> processingResult = telegramService.processing(currentMode, update);
//        return isSubscribeFuture.join() ? processingResult : new SendMessage(chatId, defaultBotAnswer.subscribeForUse());
        return telegramService.processing(currentMode, update);
    }

    private boolean checkSubscribes(long chatId) {
        String adultChannelStatus =
                telegramExecutor.checkSubscribesPositive(chatId, telegramProperties.getAccessChannelAdults());
        if (rightStatuses.contains(adultChannelStatus)) {
            return true;
        }
        String childMemberStatus =
                telegramExecutor.checkSubscribesPositive(chatId, telegramProperties.getAccessChannelChildren());
        return rightStatuses.contains(childMemberStatus);

        /* administrator (администратор канала)
         * kicked        (выгнан с канала)
         * left          (не подписан/покинул самостоятельно)
         * member        (участник канала)
         * creator       (создатель канала)
         * restricted    (доступ ограничен) */

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
