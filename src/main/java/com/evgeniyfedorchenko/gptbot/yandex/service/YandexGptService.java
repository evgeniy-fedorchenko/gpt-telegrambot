package com.evgeniyfedorchenko.gptbot.yandex.service;

import com.evgeniyfedorchenko.gptbot.configuration.properties.YandexProperties;
import com.evgeniyfedorchenko.gptbot.data.HistoryRedisService;
import com.evgeniyfedorchenko.gptbot.exception.GptTelegramBotException;
import com.evgeniyfedorchenko.gptbot.service.AiModelService;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptRequestBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
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

import static com.evgeniyfedorchenko.gptbot.configuration.OkHttpClientConfiguration.MT_APPLICATION_JSON;
import static com.evgeniyfedorchenko.gptbot.yandex.service.IamTokenSupplier.IAM_TOKEN;

@Slf4j
@Component(YandexGptService.SERVICE_NAME)
@AllArgsConstructor
public class YandexGptService implements AiModelService {

    public static final String SERVICE_NAME = "YandexGptService";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final YandexProperties yandexProperties;
    private final HistoryRedisService historyRedisService;


    @Override
    public PartialBotApiMethod<? extends Serializable> newCall(Message inputMess) {

        String userChatId = String.valueOf(inputMess.getChatId());
        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), inputMess.getText());

        List<GptMessageUnit> history = historyRedisService.getHistory(userChatId);
        history.add(question);

        GptAnswer answer = this.createPostRequest(yandexProperties.getChatbotBaseUrl(), history, GptAnswer.class)
                .orElseThrow();

        GptMessageUnit answerUnit = answer.result().alternatives().getLast().message();

        CompletableFuture.runAsync(() -> {
            historyRedisService.addMessage(userChatId, question);
            historyRedisService.addMessage(userChatId, answerUnit);
        });
        return this.getSendingObj(answer, userChatId);

    }

    private PartialBotApiMethod<? extends Serializable> getSendingObj(GptAnswer answer, String chatId) {
        String text = answer.result().alternatives().getLast().message().text();
        return new SendMessage(chatId, text);
    }

    public GptAnswer buildRequest(List<GptMessageUnit> history) {
        GptRequestBody requestBody = GptRequestBody.builder()
                .modelUri(yandexProperties.getChatbotUriPattern())
                .messages(history)
                .build();

        return webClient.post()
                .uri(yandexProperties.getChatbotBaseUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .header("x-folder-id", yandexProperties.getFolderId())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GptAnswer.class)
                .block();
    }



    public <RESP> Optional<RESP> createPostRequest(String url, Object requestBody, Class<RESP> responseType) {
        try {
            String serializedBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(url)
                    .header(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                    .header("x-folder-id", yandexProperties.getFolderId())
                    .post(RequestBody.create(serializedBody, MT_APPLICATION_JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful() && response.body() != null
                        ? Optional.of(objectMapper.readValue(response.body().string(), responseType))
                        : Optional.empty();
            }

        } catch (JsonProcessingException ex) {
            throw new GptTelegramBotException("Request was successful, but it wasn't possible to deserialize the response into an object of the \"%s\" class. Ex:{}".formatted(responseType), ex);

        } catch (IOException e) {
            throw new GptTelegramBotException("Cannot execute call of the request or read response body");
        }
    }



}
