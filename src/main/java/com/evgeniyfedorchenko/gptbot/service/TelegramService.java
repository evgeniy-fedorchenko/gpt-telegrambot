package com.evgeniyfedorchenko.gptbot.service;

import com.evgeniyfedorchenko.gptbot.aop.Log;
import com.evgeniyfedorchenko.gptbot.telegram.Mode;
import com.evgeniyfedorchenko.gptbot.telegram.TelegramBot;
import com.evgeniyfedorchenko.gptbot.telegram.TelegramExecutor;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramService {

    /**
     * Объект для планирования отправки объектов {@link SendChatAction} - т.е. для уведомления пользователя о том,
     * что в данный момент бот задан работой. Использует один поток, каждая таска должна быть запущена асинхронно
     * в виртуальном потоке, чтобы не этот шедулер не ждал ее выполнения, а приступал к следующей мгновенно
     *
     * @see TelegramService#scheduleChatAction(String chatId, Mode userMode)
     */
    private final ScheduledExecutorService singleThreadScheduler;

    private static final long SCHEDULER_RUN_TASK_PERIOD_MILLIS = 5_000L;

    private final ExecutorService executorServiceOfVirtual;
    private final ApplicationContext applicationContext;
    private final TelegramExecutor telegramExecutor;

    @Log
    public <REQ extends Serializable, RESP> PartialBotApiMethod<? extends Serializable> processing(
            Update update, Mode currentMode) {

        try {

            Message inMess = update.getMessage();
            String chatId = String.valueOf(inMess.getChatId());

            TelegramBot.localUser.set(inMess.getFrom());
            Future<?> future = scheduleChatAction(chatId, currentMode);

            AiModelService<REQ, RESP> aiModelService = getAiModelService(currentMode);

            if (aiModelService == null) {
                return null; // TODO 10.08.2024 20:15: вернуть что-то более осмысленное
            }

            REQ request = aiModelService.prepareRequest(inMess);

            String currentUrl = aiModelService.getModelUrl();
            Class<RESP> currentResponseType = aiModelService.getResponseType();

            RESP responseOpt = aiModelService.buildAndExecutePost(currentUrl, request, currentResponseType)
                    .orElseThrow();

            PartialBotApiMethod<? extends Serializable> result = aiModelService.responseProcess(responseOpt, inMess);

            future.cancel(true);
            TelegramBot.localUser.remove();
            return result;

        } catch (Exception ex) {
            log.error("Cannot processing update {}. Ex: ", update, ex);
            return null; // TODO 10.08.2024 22:44: вернуть что-то более осмысленное
        }
    }

    @SuppressWarnings("unchecked")   // safety cast in try-catch block. Return null, if impossible to cast
    private <REQ extends Serializable, RESP> @Nullable AiModelService<REQ, RESP> getAiModelService(Mode mode) {

        String serviceName = mode.getServiceName();
        if (serviceName == null) {
            return null;
        }
        Object bean = applicationContext.getBean(serviceName);
        if (bean instanceof AiModelService<?, ?> aiModelService) {
            try {
                return (AiModelService<REQ, RESP>) aiModelService;
            } catch (ClassCastException ignored) {
            }
        }

        return null;
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
}
