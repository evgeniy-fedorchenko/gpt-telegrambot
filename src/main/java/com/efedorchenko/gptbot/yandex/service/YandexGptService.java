package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.OkHttpClientConfiguration;
import com.efedorchenko.gptbot.configuration.properties.DefaultBotAnswer;
import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.efedorchenko.gptbot.data.HistoryRedisService;
import com.efedorchenko.gptbot.service.AiModelService;
import com.efedorchenko.gptbot.utils.logging.Log;
import com.efedorchenko.gptbot.yandex.model.GptAnswer;
import com.efedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.efedorchenko.gptbot.yandex.model.GptRequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import static com.efedorchenko.gptbot.utils.logging.LogUtils.LOGIC_MARKER;

@Slf4j
@RequiredArgsConstructor
@Component(YandexGptService.SERVICE_NAME)
public class YandexGptService implements AiModelService<GptRequestBody, GptAnswer> {

    public static final String SERVICE_NAME = "YandexGptService";
    private static final int MAX_COUNT_SYMBOLS = 3500;
    private static final long REQUIRED_MILLIS_BETWEEN_REQS = 1000L;
    private static final Semaphore requestSemaphore = new Semaphore(5, true);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HistoryRedisService historyCache;
    private final ExecutorService executorOfVirtual;
    private final DefaultBotAnswer defaultBotAnswer;
    private final YandexProperties yandexProperties;

    @Override
    public String validate(Message inputMess) {
        String messText = inputMess.getText();
        return messText.length() > MAX_COUNT_SYMBOLS
                ? messText.substring(messText.length() - MAX_COUNT_SYMBOLS)
                : messText;
    }

    @Override
    @Log(Level.TRACE)
    public GptRequestBody prepareRequest(Message inputMess) {

        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), inputMess.getText());
        List<GptMessageUnit> history = historyCache.getHistory(inputMess.getChatId());
        history.add(question);

        CompletableFuture.runAsync(() -> historyCache.addMessage(inputMess.getChatId(), question), executorOfVirtual);
        return GptRequestBody.builder()
                .modelUri(yandexProperties.getChatbotUri().formatted())
                .messages(history)
                .build();
    }

    @Override
    @Log(Level.TRACE)
    public Optional<GptAnswer> buildAndExecutePost(String url, Serializable requestBody, Class<GptAnswer> responseType)
            throws IOException {

        String serializedBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(url)
                .header(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .header(YandexProperties.FOLDER_ID_HEADER_NAME, yandexProperties.getFolderId())
                .post(RequestBody.create(serializedBody, OkHttpClientConfiguration.MT_APPLICATION_JSON))
                .build();

        try (Response response = doExecuteControlSpeed(request)) {

            if (response == null || response.code() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return Optional.of(GptAnswer.builder().errorHttpStatus(HttpStatus.TOO_MANY_REQUESTS).build());
            }
            return response.body() != null
                    ? Optional.of(objectMapper.readValue(response.body().string(), responseType))
                    : Optional.of(GptAnswer.builder().errorHttpStatus(HttpStatus.BAD_GATEWAY).build());
        }
    }

    @Override
    @Log(Level.TRACE)
    public PartialBotApiMethod<? extends Serializable> responseProcess(GptAnswer response, Message sourceMess) {

        String chatId = String.valueOf(sourceMess.getChatId());
        HttpStatus errorHttpStatus = response.getErrorHttpStatus();

        if (errorHttpStatus != null) {
            return new SendMessage(chatId, defaultBotAnswer.yagptAnswerOfStatus(errorHttpStatus));
        }

        if (response.getResult() == null) {
            log.error(LOGIC_MARKER, "answer is null. Response: {}", response);
            return new SendMessage(chatId, defaultBotAnswer.unknownError());
        }
        GptMessageUnit answer = response.getResult().getAlternatives().getLast().getMessage();
        CompletableFuture.runAsync(() -> historyCache.addMessage(chatId, answer), executorOfVirtual);
        return new SendMessage(chatId, answer.getText());
    }

    @Override
    public String getModelUrl() {
        return yandexProperties.getChatbotBaseUrl();
    }

    @Override
    public Class<GptAnswer> getResponseType() {
        return GptAnswer.class;
    }

    @Nullable
    private Response doExecuteControlSpeed(Request request) throws IOException {

        try {

        /* Если модель полностью загружена (обрабатывает 5 запросов сразу), то после получения ответа на один
           из запросов необходимо выждать немного, так как на стороне Яндекса запрос освобождается не сразу */
            boolean needsWait = false;
            Call call = httpClient.newCall(request);

            if (requestSemaphore.availablePermits() == 0) {
                needsWait = true;
            }
            requestSemaphore.acquire();
            if (needsWait) {
                Thread.sleep(REQUIRED_MILLIS_BETWEEN_REQS);
            }
            return call.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            requestSemaphore.release();
        }
    }
}
