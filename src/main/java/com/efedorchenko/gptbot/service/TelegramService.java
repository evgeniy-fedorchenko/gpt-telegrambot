package com.efedorchenko.gptbot.service;

import com.efedorchenko.gptbot.aop.Log;
import com.efedorchenko.gptbot.data.UserModeRedisService;
import com.efedorchenko.gptbot.exception.RetryAttemptNotReadyException;
import com.efedorchenko.gptbot.telegram.Mode;
import com.efedorchenko.gptbot.telegram.TelegramBot;
import com.efedorchenko.gptbot.telegram.TelegramExecutor;
import com.efedorchenko.gptbot.yandex.service.SpeechRecogniser;
import com.efedorchenko.gptbot.yandex.service.YandexArtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramService {

    /**
     * Объект для планирования отправки объектов {@link SendChatAction} - т.е. для уведомления пользователя о том,
     * что в данный момент бот задан работой. Использует один поток, каждая таска должна быть запущена асинхронно
     * в виртуальном потоке, чтобы этот шедулер не ждал ее выполнения, а приступал к следующей мгновенно
     *
     * @see TelegramService#scheduleChatAction(String chatId, Mode userMode)
     */
    private final ScheduledExecutorService singleThreadScheduler;

    private static final long SCHEDULER_RUN_TASK_PERIOD_MILLIS = 5_000L;

    private final ExecutorService executorServiceOfVirtual;
    private final ApplicationContext applicationContext;
    private final UserModeRedisService userModeCache;
    private final TelegramExecutor telegramExecutor;
    private final SpeechRecogniser speechRecogniser;

    @Log(result = false)   // result логируется методом, стоящим выше по стеку
    public <REQ extends Serializable, RESP> PartialBotApiMethod<? extends Serializable> processing(
            Mode currentMode, Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());
        Future<?> future = scheduleChatAction(chatId, currentMode);
        TelegramBot.localUser.set(inMess.getFrom());

        try {
            AiModelService<REQ, RESP> aiModelService = getAiModelService(currentMode);
            if (inMess.hasVoice()) {
                byte[] voiceBytes = telegramExecutor.downloadVoice(inMess.getVoice());
                Optional<String> recognizeOpt = speechRecogniser.recognize(voiceBytes);
                if (recognizeOpt.isEmpty()) {
                    return new SendMessage(chatId, "{a.b}");
                }
            }
            inMess.setText(aiModelService.validate(inMess));

            REQ request = aiModelService.prepareRequest(inMess);
            String currentUrl = aiModelService.getModelUrl();
            Class<RESP> responseType = aiModelService.getResponseType();

            RESP responseOpt = aiModelService.buildAndExecutePost(currentUrl, request, responseType).orElseThrow();
            return aiModelService.responseProcess(responseOpt, inMess);

        } catch (Exception ex) {
            return handleException(ex, update);

        } finally {
            future.cancel(true);
            TelegramBot.localUser.remove();
            if (userModeCache.getMode(chatId).equals(Mode.YANDEX_ART_HOLD)) {
                userModeCache.setMode(chatId, Mode.YANDEX_ART);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <REQ extends Serializable, RESP> AiModelService<REQ, RESP> getAiModelService(Mode mode) {
        return (AiModelService<REQ, RESP>) applicationContext.getBean(
                Objects.requireNonNullElse(mode.getServiceName(), YandexArtService.SERVICE_NAME), AiModelService.class);
    }

    private Future<?> scheduleChatAction(String chatId, Mode mode) {

        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(chatId);
        sendChatAction.setAction(mode.getActionType());

        return singleThreadScheduler.scheduleAtFixedRate(() ->
                        CompletableFuture.runAsync(() -> telegramExecutor.send(sendChatAction), executorServiceOfVirtual),
                0,
                SCHEDULER_RUN_TASK_PERIOD_MILLIS,
                TimeUnit.MILLISECONDS
        );
    }

    private PartialBotApiMethod<? extends Serializable> handleException(Exception thrown, Update update) {

        String chatId = String.valueOf(update.getMessage().getChatId());

        switch (thrown) {
            case IllegalStateException ise -> {
                log.error(NETWORK_MARKER, "IllegalStateException -> Update: {}\nEx: ", update, ise);
                return new SendMessage(chatId, ISE_ANSWER);
            }
            case NullPointerException npe -> {
                log.error(POWER_MARKER, "NullPointerException -> Update: {}\nEx: ", update, npe);
                return new SendMessage(chatId, NPE_ANSWER);
            }

//            Картинка не успела сгенериться за отведенные 6 минут
            case RetryAttemptNotReadyException ranre -> {
                log.warn(RANRE_MARKER, ranre.getMessage());

                return new SendMessage(chatId, RANRE_ANSWER);
            }

            case JsonProcessingException jpe -> {
                log.error(LOGIC_MARKER, "JsonProcessingException -> Update: {}\nEx: ", update, jpe);
                return new SendMessage(chatId, JPE_ANSWER);
            }
            case IOException ioe -> {
                Thread.dumpStack();   // Maybe this is a OutOfMemoryError. Answer of model is too large
                log.error(NETWORK_MARKER, "IOException was thrown. Dump of stack is above, maybe. Update: {}.\nEx: ", update, ioe);
                return new SendMessage(chatId, COMMON_ANSWER);
            }
            default -> {
                log.error(LOGIC_MARKER, "Cannot processing update, unexpected exception. Update: {}. Ex: ", update, thrown);
                return new SendMessage(chatId, COMMON_ANSWER);
            }
        }
    }


//    Маркеты для log.error и сообщения для юзеров, отправляемые в случае ошибок

    private static final Marker LOGIC_MARKER = MarkerFactory.getMarker("LOGIC");
    private static final Marker POWER_MARKER = MarkerFactory.getMarker("POWER");
    private static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
    private static final Marker RANRE_MARKER = MarkerFactory.getMarker("RetryAttemptNotReadyException");
    private static final String JPE_ANSWER = "Упс!\nПри обработке вашего запроса (или ответа) что-то пошло не так! :(\nМы записали эту ошибку, но будем вам очень признательны, если дадите кратенькую обратную связь.\nИспользуйте для этого -> /feedback\n\nТолько добра ❤\uFE0F";
    private static final String ISE_ANSWER = "Упс!\nПри отправке вашего запроса что-то пошло не так! :(\nМы записали эту ошибку, но будем вам очень признательны, если дадите кратенькую обратную связь.\nИспользуйте для этого -> /feedback\n\nТолько добра ❤\uFE0F";
    private static final String COMMON_ANSWER = "Упс!\nПри обработке запроса произошла непредвиденная ситуация, пока что не могу сказать, что случилось :(\nМы записали эту ошибку, но будем вам очень признательны, если дадите кратенькую обратную связь.\nИспользуйте для этого -> /feedback\n\nТолько добра ❤\uFE0F";
    private static final String NPE_ANSWER = "Упс!\nКажется, у нас сбой питания на сервере, но мы работаем на этим. :(\nМы записали эту ошибку, но если вам не все равно - дайте нам знать - будем вам очень признательны\nИспользуйте для этого -> /feedback\n\nТолько добра ❤\uFE0F";
    private static final String RANRE_ANSWER = "Прости, но что-то не получается создать картинку по такому описанию в срок :(\nТы можешь написать нам и мы проверим твое изображение в ручную\nИспользуйте для этого -> /feedback\n\nТолько добра ❤\uFE0F";

}
