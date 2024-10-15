package com.efedorchenko.gptbot.service;

import com.efedorchenko.gptbot.configuration.properties.DefaultBotAnswer;
import com.efedorchenko.gptbot.data.UserModeRedisService;
import com.efedorchenko.gptbot.exception.GptTelegramBotException;
import com.efedorchenko.gptbot.exception.RetryAttemptNotReadyException;
import com.efedorchenko.gptbot.telegram.Mode;
import com.efedorchenko.gptbot.telegram.TelegramExecutor;
import com.efedorchenko.gptbot.utils.Helper;
import com.efedorchenko.gptbot.utils.logging.Log;
import com.efedorchenko.gptbot.yandex.model.SpeechKitAnswer;
import com.efedorchenko.gptbot.yandex.model.VoiceRecResult;
import com.efedorchenko.gptbot.yandex.service.SpeechRecogniser;
import com.efedorchenko.gptbot.yandex.service.YandexArtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

import static com.efedorchenko.gptbot.utils.logging.LogUtils.*;

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

    private final DefaultBotAnswer defaultBotAnswer;
    private final SpeechRecogniser speechRecogniser;
    private final TelegramExecutor telegramExecutor;
    private final UserModeRedisService userModeCache;
    private final ApplicationContext applicationContext;
    private final ExecutorService executorServiceOfVirtual;

    @Log
    public <REQ extends Serializable, RESP> PartialBotApiMethod<? extends Serializable> processing(
            Mode currentMode, Update update) {

        Message inMess = update.getMessage();
        String chatId = String.valueOf(inMess.getChatId());
        Future<?> future = scheduleChatAction(chatId, currentMode);

        try {
            AiModelService<REQ, RESP> aiModelService = getAiModelService(currentMode);

            if (inMess.hasVoice()) {
                VoiceRecResult voiceRecResult = recogniseVoice(inMess.getVoice());
                if (voiceRecResult.getAnswerToErrorMessage() != null) {
                    return new SendMessage(chatId, voiceRecResult.getAnswerToErrorMessage());
                }
                inMess.setText(voiceRecResult.getRecognizedMessage());
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
            if (userModeCache.getMode(chatId).equals(Mode.YANDEX_ART_HOLD)) {
                userModeCache.setMode(chatId, Mode.YANDEX_ART);
            }
        }
    }

    private VoiceRecResult recogniseVoice(Voice voice) {

        if (voice.getDuration() >= 30) {
            return VoiceRecResult.builder().answerToErrorMessage(defaultBotAnswer.voiceIsLongerThan30s()).build();
        }
        byte[] voiceBytes = telegramExecutor.downloadVoice(voice);
        Optional<SpeechKitAnswer> recognizeOpt = speechRecogniser.doRecognize(voiceBytes);
        if (recognizeOpt.isEmpty()) {
            return VoiceRecResult.builder().answerToErrorMessage(defaultBotAnswer.couldNotRecognizeVoice()).build();
        }
        SpeechKitAnswer recognized = recognizeOpt.get();
        if (recognized.getErrorMessage() != null) {
            return VoiceRecResult.builder().answerToErrorMessage(recognized.getErrorMessage()).build();
        }
        return VoiceRecResult.builder().recognizedMessage(recognized.getResult()).build();
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
                log.error(NETWORK_MARKER, "IllegalStateException -> Update: {}\nEx: ", Helper.write(update), ise);
                return new SendMessage(chatId, defaultBotAnswer.illegalStateEx());
            }
            case NullPointerException npe -> {
                log.error(POWER_MARKER, "NullPointerException -> Update: {}\nEx: ", Helper.write(update), npe);
                return new SendMessage(chatId, defaultBotAnswer.nullPointerEx());
            }

//            Картинка не успела сгенериться за отведенные 6 минут
            case RetryAttemptNotReadyException ranre -> {
                log.warn(RANRE_MARKER, ranre.getMessage(), ranre);
                return new SendMessage(chatId, defaultBotAnswer.retryAttemptNotReadyEx());
            }

            case JsonProcessingException jpe -> {
                log.error(LOGIC_MARKER, "JsonProcessingException -> Update: {}\nEx: ", Helper.write(update), jpe);
                return new SendMessage(chatId, defaultBotAnswer.jsonProcessingEx());
            }
            case IOException ioe -> {
                Thread.dumpStack();   // Maybe this is a OutOfMemoryError. Answer of model is too large
                log.error(NETWORK_MARKER, "IOException was thrown. Dump of stack is above, maybe. Update: {}.\nEx: ", Helper.write(update), ioe);
                return new SendMessage(chatId, defaultBotAnswer.otherEx());
            }
            case GptTelegramBotException gtbe -> {
                log.error(LOGIC_MARKER, "GptTelegramBotException. Mess: {}\nUpdate: {}\nCause: ", gtbe.getMessage(), Helper.write(update), gtbe.getCause());
                return new SendMessage(chatId, defaultBotAnswer.otherEx());
            }
            default -> {
                log.error(LOGIC_MARKER, "Cannot processing update, unexpected exception. Update: {}. Ex: ", Helper.write(update), thrown);
                return new SendMessage(chatId, defaultBotAnswer.otherEx());
            }
        }
    }

}
