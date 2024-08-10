package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.data.UserModeRedisService;
import com.evgeniyfedorchenko.gptbot.service.TelegramService;
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

import static com.evgeniyfedorchenko.gptbot.telegram.TelegramDistributor.Command.*;

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

    private final UserModeRedisService userModeRedisService;
    private final TelegramService telegramService;

    public TelegramDistributor(UserModeRedisService userModeRedisService,
                               TelegramService telegramService) {
        this.userModeRedisService = userModeRedisService;
        this.telegramService = telegramService;

        this.unsupportedCommandReact = chatId -> new SendMessage(chatId, "Command is not supported");
        this.commandReactMap = Map.of(
                START.getRepresentation(), chatId -> new SendMessage(chatId, START.getAnswerTest()),
                HELP.getRepresentation(), chatId -> new SendMessage(chatId, HELP.getAnswerTest()),
                FEEDBACK.getRepresentation(), chatId -> new SendMessage(chatId, FEEDBACK.getAnswerTest()),

                YA_GPT.getRepresentation(), chatId -> {
                    userModeRedisService.setMode(chatId, Mode.YANDEX_GPT);
                    return new SendMessage(chatId, YA_GPT.getAnswerTest());
                },
                YA_ART.getRepresentation(), chatId -> {
                    userModeRedisService.setMode(chatId, Mode.YANDEX_ART);
                    return new SendMessage(chatId, YA_ART.getAnswerTest());
                }
        );
    }

    public PartialBotApiMethod<? extends Serializable> distribute(Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());

//        Block if user wait for the image as YANDEX_ART mode
        Mode currentMode = userModeRedisService.getMode(chatId); // Для аварийного сброса: userModeRedisService.setMode(chatId, Mode.YANDEX_ART)
        if (currentMode.equals(Mode.YANDEX_ART_HOLDED)) {
            return new SendMessage(chatId, "Не торопись, подожди еще немного, окей?\nНадо завершить предыдущую генерацию");
        }

//        Handle commands
        if (inMess.isCommand()) {
            return commandReactMap.getOrDefault(inMess.getText(), unsupportedCommandReact).apply(chatId);
        }

//        Main processing
        return telegramService.processing(update, currentMode);
    }

    @Getter
    @AllArgsConstructor
    public enum Command {

        START("/start", "Привет, как жизнь?\nИспользуй команды слева от поля ввода и вперед! Режим по умолчанию: gpt"),
        HELP("/help", """
                *Нейрон GPT v. 0.0.1 SNAPSHOT*
                                
                Привет! Это Telegram-бот центра ИИ [Нейрон](https://aiperm.ru/) 😎
                                
                Он основан на современных моделях искусственного интеллекта и готов помочь тебе в решении ваших задач! Чтобы выбрать режим, воспользуйся кнопками слева от поля ввода:
                                
                - *Режим gpt* 💬
                Включен по умолчанию. Подходит для использования языковой модели в режиме чата. Ты можешь задавать вопросы и получать ответы от нейросети. Она запоминает контекст разговора и может помочь во многих вопросах (используй ее как будто это обычный чат).
                                
                - *Режим art* 🌌
                Режим для создания изображений. Подготовь одно сообщение (промпт) и опиши в нем, что ты хочешь получить, затем отправь его и подожди a little bit. Когда изображение будет готово, ты сможешь получить его прямо в этом чате, я буду держать тебя в курсе прогресса. Если тебя не устроил результат, попробуй изменить описание и повтори попытку. Подробнее о том, как лучше всего составлять промпты: %s.
                                
                Мы собираемся улучшать эти модели и добавить новые в будущем, так что следи за обновлениями!
                И это, если обнаружишь ошибку... что ж, ты неплохо постарался, потому что мы не смогли этого сделать! Используй /feedback, чтобы сообщить нам об этом, и мы исправим ее в будущих версиях.
                                
                Если у тебя есть еще какие-либо вопросы или предложения, просто напиши нам несколько строк здесь: @AzorAhai777 или @alexsubbotinn
                                
                Удачи! ⭐️
                """),
        FEEDBACK("/feedback", """
                Ого, ты очень крут! Если тебе есть что сказать, обязательно напиши нам, например ты можешь:
                - Поделиться пожеланиями по улучшению
                - Просто оставить отзыв
                - Задать вопрос
                - Рассказать о неисправности
                
                То не стесняйся и обязательно напиши сюда: @AzorAhai777 или сюда: @alexsubbotinn
                Thank you bro!"),
                """),
        YA_GPT("/gpt", "Ок, режим \"GPT\" включен"),
        YA_ART("/art", "Ок, режим \"ART\" включен");

        private final String representation;
        private final String answerTest;

    }
}
