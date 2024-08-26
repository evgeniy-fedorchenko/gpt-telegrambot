package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.OkHttpClientConfiguration;
import com.efedorchenko.gptbot.configuration.RetryTemplateConfiguration;
import com.efedorchenko.gptbot.configuration.properties.DefaultBotAnswer;
import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.efedorchenko.gptbot.data.UserModeRedisService;
import com.efedorchenko.gptbot.exception.GptTelegramBotException;
import com.efedorchenko.gptbot.exception.RetryAttemptNotReadyException;
import com.efedorchenko.gptbot.service.AiModelService;
import com.efedorchenko.gptbot.telegram.Mode;
import com.efedorchenko.gptbot.telegram.TelegramExecutor;
import com.efedorchenko.gptbot.utils.logging.Log;
import com.efedorchenko.gptbot.yandex.model.ArtAnswer;
import com.efedorchenko.gptbot.yandex.model.ArtMessageUnit;
import com.efedorchenko.gptbot.yandex.model.ArtRequestBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
@Component(YandexArtService.SERVICE_NAME)
public class YandexArtService implements AiModelService<ArtRequestBody, ArtAnswer> {

    public static final String SERVICE_NAME = "YandexArtService";

    /**
     * Карта маппинга нестандартных ответов сети {@code YandexART} и ответов этого бота.<br>
     * Нестандартным считается ответ, когда нейросеть отказалась генерировать изображение по промпту по
     * различным причинам, например из-за аморального содержания промпта или нарушения её политики использования.
     * Такие ответы приходят со статусом {@link HttpStatus#BAD_REQUEST} и непустым полем
     * {@link ArtAnswer#getErrorDisc()}. Эта карта соотносит ответы сети из {@link ArtAnswer#getErrorDisc()}
     * и текстом которым ответит этот бот на такой запрос
     * <lu>
     * <li><b>key</b> - текст ответа нейронки, в случае отказа генерировать изображение</li>
     * <li><b>value</b>- ответ, который этот бот отправил юзеру</li>
     * </lu>
     */
    private static final Map<String, String> FILED_MODEL_ANSWER_MAP = Map.of(
            "it is not possible to generate an image from this request because it may violate the terms of use", "Упс! Такое генерировать не буду!"
    );

    /**
     * Дефолтный ответ бота, в случае, если нейросеть отказалась генерировать изображение по заданному промпту по
     * неизвестной причине. Известные причины (и соотносящиеся с ними ответы бота) определены в карте
     * {@link YandexArtService#FILED_MODEL_ANSWER_MAP}
     */
    private static final String DEFAULT_FILED_MODEL_ANSWER_MAP_VALUE = "Прости, но нейросеть отказалась генерировать изображение по такому промпту, попробуй как-нибудь изменить его";
    private static final int MAX_COUNT_SYMBOLS = 500;

    /**
     * Счетчик процентов. Показывает прогресс генерации изображения. На самом деле не имеет связи с процессом
     * генерации, а просто постепенно увеличивается с все замедляющейся скоростью, никогда не достигая {@code 100%},
     * расчет значения происходит в методе {@link YandexArtService#calculatePercentReady(double current)}<br>
     * Значение в начале генерации - {@code 1%}<br>
     * После каждой генерации сбрасывается на {@code 1%}
     */
    private double percentReady = 1;

    /**
     * Сообщение, отправляемое юзеру, для уведомления его о процессе генерации. Отправляемое сообщение - объект
     * {@link EditMessageText}, после отправки возвращающий этот объект. Поле нужно для хранения контекста этого
     * сообщения, включающего {@link Message#getMessageId()}. Необходимо, чтобы держать юзера в курсе о процессе
     * генерации, т.к. это занимает продолжительное время. Как правило, это сообщение содержит текст
     * {@code Генерация завершена на X%}. В качестве счетчика процентов выступает поле
     * {@link YandexArtService#percentReady}, который рассчитывается на основе прогресса генерации
     */
    private Message generationProcessMess;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;
    private final YandexProperties yandexProperties;
    private final TelegramExecutor telegramExecutor;
    private final DefaultBotAnswer defaultBotAnswer;
    private final UserModeRedisService userModeCache;
    private final ExecutorService executorServiceOfVirtual;
    private final RetryTemplateConfiguration retryTemplateConfiguration;

    @Override
    public String validate(Message inputMess) {
        String prompt = inputMess.getText();
        return prompt.length() > MAX_COUNT_SYMBOLS
                ? prompt.substring(prompt.length() - MAX_COUNT_SYMBOLS)
                : prompt;
    }

    @Override
    public ArtRequestBody prepareRequest(Message inputMess) {

        userModeCache.setMode(inputMess.getChatId(), Mode.YANDEX_ART_HOLD);

        return ArtRequestBody.builder()
                .modelUri(yandexProperties.getArtModelUri())
                .messages(Collections.singletonList(ArtMessageUnit.builder().text(inputMess.getText()).build()))
                .build();
    }

    @Log
    @Override
    public Optional<ArtAnswer> buildAndExecutePost(String url, Serializable requestBody, Class<ArtAnswer> responseType)
            throws IOException {

        String serializedBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .header(YandexProperties.YA_RQUID_HEADER_NAME, MDC.get("RqUID"))
                .header(YandexProperties.FOLDER_ID_HEADER_NAME, yandexProperties.getFolderId())
                .post(RequestBody.create(serializedBody, OkHttpClientConfiguration.MT_APPLICATION_JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.body() != null
                    ? Optional.of(objectMapper.readValue(response.body().string(), responseType))
                    : Optional.empty();
        }
    }

    @Log(result = false)
    @Override
    public PartialBotApiMethod<? extends Serializable> responseProcess(ArtAnswer firstResponse, Message sourceMess) {

        String chatId = String.valueOf(sourceMess.getChatId());
        if (firstResponse.hasErrors()) {
            userModeCache.setMode(chatId, Mode.YANDEX_ART);
            return this.generateFiled(firstResponse, sourceMess);
        }

        if (firstResponse.getId() != null) {
            generationProcessMess =
                    telegramExecutor.sendAndReturn(new SendMessage(chatId, defaultBotAnswer.yaartRequestAccepted()));
        } else {
            userModeCache.setMode(chatId, Mode.YANDEX_ART);
            return new SendMessage(chatId, defaultBotAnswer.unknownError());
        }

        ArtAnswer secondResponse = retryTemplate.execute(context -> {
                    if (context.getRetryCount() == retryTemplateConfiguration.getMaxAttempts()) {
                        percentReady = 1;
                    }
                    return this.processRetryInvoke(this.retryInvoke(firstResponse.getId()), chatId)
                            .orElseThrow(() -> new RetryAttemptNotReadyException("The picture is not ready yet"));
                }
        );

        return secondResponse.hasErrors()
                ? generateFiled(secondResponse, sourceMess)
                : generateComplete(secondResponse, chatId);
    }

    @Override
    public String getModelUrl() {
        return yandexProperties.getArtModelBaseUrl();
    }

    @Override
    public Class<ArtAnswer> getResponseType() {
        return ArtAnswer.class;
    }

    private ArtAnswer retryInvoke(String operationId) {
        try {

            Request request = new Request.Builder()
                    .url(yandexProperties.getArtModelCompleteUrlPattern().formatted(operationId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                    .header(YandexProperties.YA_RQUID_HEADER_NAME, MDC.get("RqUID"))
                    .header(YandexProperties.FOLDER_ID_HEADER_NAME, yandexProperties.getFolderId())
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return objectMapper.readValue(response.body().string(), ArtAnswer.class);
            }

        } catch (JsonProcessingException ex) {
            throw new GptTelegramBotException("Request was successful, but it wasn't possible to deserialize the response into an object of the \"%s\" class. Ex:{}".formatted(ArtAnswer.class), ex);

        } catch (IOException e) {
            throw new GptTelegramBotException("Cannot execute call of the request or read response body");
        }
    }

    private Optional<ArtAnswer> processRetryInvoke(ArtAnswer maybeReady, String chatId) {
        if (maybeReady != null && maybeReady.isDone()) {

            CompletableFuture.runAsync(() -> {
                telegramExecutor.send(new DeleteMessage(chatId, generationProcessMess.getMessageId()));
                userModeCache.setMode(chatId, Mode.YANDEX_ART);
                percentReady = 1;
            }, executorServiceOfVirtual);

            return Optional.of(maybeReady);

        } else {
            CompletableFuture.runAsync(() -> {
                Double newPercentReady = calculatePercentReady(percentReady);
                if (!newPercentReady.equals(percentReady)) {
                    percentReady = newPercentReady;
                    EditMessageText mess = new EditMessageText("Генерация завершена на %.2f%%".formatted(percentReady));
                    mess.setChatId(chatId);
                    mess.setMessageId(generationProcessMess.getMessageId());
                    telegramExecutor.send(mess);
                }

            }, executorServiceOfVirtual);

            return Optional.empty();
        }
    }

    private SendMessage generateFiled(ArtAnswer completedAnswer, Message sourceMess) {
        log.error("Filed generate image. Prompt: {}. user: {}", sourceMess.getText(), sourceMess.getChatId());
        return new SendMessage(
                String.valueOf(sourceMess.getChatId()),
                FILED_MODEL_ANSWER_MAP.getOrDefault(completedAnswer.getErrorDisc(), DEFAULT_FILED_MODEL_ANSWER_MAP_VALUE)
        );
    }

    private SendPhoto generateComplete(ArtAnswer answer, String chatId) {
        byte[] bytes = Base64.getDecoder().decode(answer.getResponse().getImage());
        InputFile result = new InputFile(new ByteArrayInputStream(bytes), "result");

        return new SendPhoto(chatId, result);
    }

    private Double calculatePercentReady(double current) {
        double logBase;

        if (current < 98.7) {

            if (current < 30) {
                logBase = 2;
            } else if (current < 60) {
                logBase = 2.5;
            } else if (current < 80) {
                logBase = 3;
            } else {
                logBase = 10;
            }
            double calculated = current + Math.log(100 - current) / Math.log(logBase);
            return calculated < 80
                    ? calculated + Math.random() * 2
                    : calculated;

        } else if (current < 99.37) {
            return current + (100 - current) / 5;

        } else if (current < 99.99) {
            return current + 0.01;

        } else {
            return 99.99;
        }
    }
}
