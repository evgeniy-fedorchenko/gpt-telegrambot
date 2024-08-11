package com.evgeniyfedorchenko.gptbot.yandex.service;

import com.evgeniyfedorchenko.gptbot.configuration.properties.YandexProperties;
import com.evgeniyfedorchenko.gptbot.data.HistoryRedisService;
import com.evgeniyfedorchenko.gptbot.service.AiModelService;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptRequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
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

import static com.evgeniyfedorchenko.gptbot.configuration.OkHttpClientConfiguration.MT_APPLICATION_JSON;

@RequiredArgsConstructor
@Component(YandexGptService.SERVICE_NAME)
public class YandexGptService implements AiModelService<GptRequestBody, GptAnswer> {

    public static final String SERVICE_NAME = "YandexGptService";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final HistoryRedisService historyCache;
    private final YandexProperties yandexProperties;
    private final ExecutorService executorServiceOfVirtual;

    @Override
    public GptRequestBody prepareRequest(Message inputMess) {

        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), inputMess.getText());
        List<GptMessageUnit> history = historyCache.getHistory(inputMess.getChatId());
        history.add(question);

        CompletableFuture.runAsync(
                () -> historyCache.addMessage(inputMess.getChatId(), question),
                executorServiceOfVirtual
        );
        return GptRequestBody.builder()
                .modelUri(yandexProperties.getChatbotUriPattern().formatted())
                .messages(history)
                .build();
    }

    @Override
    public Optional<GptAnswer> buildAndExecutePost(String url, Object requestBody, Class<GptAnswer> responseType)
            throws IOException {

        String serializedBody = objectMapper.writeValueAsString(requestBody);

        Request request = new Request.Builder()
                .url(url)
                .header(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .header("x-folder-id", yandexProperties.getFolderId())
                .post(RequestBody.create(serializedBody, MT_APPLICATION_JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.body() != null
                    ? Optional.of(objectMapper.readValue(response.body().string(), responseType))
                    : Optional.empty();
        }
    }

    @Override
    public PartialBotApiMethod<? extends Serializable> responseProcess(GptAnswer response, Message sourceMess) {
        String chatId = String.valueOf(sourceMess.getChatId());
        GptMessageUnit answer = response.result().alternatives().getLast().message();

        CompletableFuture.runAsync(
                () -> historyCache.addMessage(chatId, answer),
                executorServiceOfVirtual
        );

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
}
