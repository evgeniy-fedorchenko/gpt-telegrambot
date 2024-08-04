package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.data.UserModeRedisService;
import com.evgeniyfedorchenko.gptbot.service.AiModelService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
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

    private final UserModeRedisService userModeRedisService;
    private final ApplicationContext applicationContext;
    private final Map<String, Function<String, SendMessage>> commandReactMap;
    private final Function<String, SendMessage> unsupportedCommandReact;

    public TelegramDistributor(UserModeRedisService userModeRedisService,
                               ApplicationContext applicationContext) {
        this.userModeRedisService = userModeRedisService;
        this.applicationContext = applicationContext;

        this.commandReactMap = Map.of(
                START.getRepresentation(), t -> new SendMessage(t, START.getAnswerTest()),
                HELP.getRepresentation(), t -> new SendMessage(t, HELP.getAnswerTest()),
                BUG_REPORT.getRepresentation(), t -> new SendMessage(t, BUG_REPORT.getAnswerTest()),

                YA_GPT.getRepresentation(), t -> {
                    userModeRedisService.setMode(t, Mode.YANDEX_GPT);
                    return new SendMessage(t, YA_GPT.getAnswerTest());
                },
                YA_ART.getRepresentation(), t -> {
                    userModeRedisService.setMode(t, Mode.YANDEX_ART);
                    return new SendMessage(t, YA_ART.getAnswerTest());
                }
        );
        this.unsupportedCommandReact = t -> new SendMessage(t, "Command is not supported");
    }

    public PartialBotApiMethod<? extends Serializable> distribute(Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());

//        Block if user wait for the image as YANDEX_ART mode
        if (userModeRedisService.getMode(chatId).equals(Mode.YANDEX_ART_HOLDED)) {
            return new SendMessage(chatId, "Just give it a few more seconds, okay?");
        }

//        Handle commands
        if (inMess.isCommand()) {
            return commandReactMap.getOrDefault(inMess.getText(), unsupportedCommandReact)
                    .apply(chatId);
        }

//        Main processing
        AiModelService aiModelService = getAiModelService(inMess.getChatId());
        return aiModelService.newCall(inMess);
    }

    private AiModelService getAiModelService(Long chatId) {
        Mode mode = userModeRedisService.getMode(String.valueOf(chatId));
        return ((AiModelService) applicationContext.getBean(mode.getServiceName()));
    }

    @Getter
    @AllArgsConstructor
    public enum Command {

        START("/start", "Привет, как жизнь?\nИспользуй команды слева от поля ввода и вперед! Режим по умолчанию: gpt"),
        HELP("/help", """
                **Нейрон GPT v. 0.0.1 SNAPSHOT**
                
                Привет! Это Telegram-бот онлайн-школы "Нейрон"
                
                Он основан на современных моделях искусственного интеллекта и готов помочь тебе в решении ваших задач! Чтобы выбрать режим, воспользуйся кнопками слева от поля ввода:
                
                - **gpt**: (по умолчанию) для использования языковой модели в режиме чата. Ты можешь задавать вопросы и получать ответы от нейросети. Она запоминает контекст разговора и может помочь во многих вопросах (используй ее как будто это обычный чат).
                
                - **art**: режим для создания изображений. Подготовь одно сообщение (промпт) и опиши в нем, что ты хочешь получить, затем отправь его и подожди a little bit. Когда изображение будет готово, ты сможешь получить его прямо в этом чате, я буду держать тебя в курсе прогресса. Если тебя не устроил результат, попробуй изменить описание и повтори попытку. Подробнее о том, как лучше всего составлять промпты: %s.
                                
                Мы собираемся улучшать эти модели и добавить новые в будущем, так что следи за обновлениями!
                И это, если обнаружишь ошибку... что ж, ты неплохо постарался, потому что мы не смогли этого сделать! Используй "/bugreport", чтобы сообщить нам об этом, и мы исправим ее в будущих версиях.
                                
                Если у тебя есть еще какие-либо вопросы или предложения, просто напиши нам несколько строк здесь: @AzorAhai777 или @alexsubbotinn
                                
                Удачи!
                """),
        BUG_REPORT("/bugreport", "Ого, ты очень крут! Расскажешь нам об этом? Просто напиши сюда: @AzorAhai777 или сюда: @alexsubbotinn\nThank you bro!"),
        YA_GPT("/gpt", "Ок, режим \"GPT\" включен"),
        YA_ART("/art", "Ок, режим \"ART\" включен");

        private final String representation;
        private final String answerTest;

    }
}
