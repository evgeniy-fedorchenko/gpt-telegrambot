package com.evgeniyfedorchenko.gptbot.yandex.service;

import com.evgeniyfedorchenko.gptbot.data.HistoryRedisService;
import com.evgeniyfedorchenko.gptbot.service.AiModelService;
import com.evgeniyfedorchenko.gptbot.yandex.YandexProperties;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.model.GptRequestBody;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component(YandexGptService.SERVICE_NAME)
@AllArgsConstructor
public class YandexGptService implements AiModelService {

    public static final String SERVICE_NAME = "YandexGptService";
    protected static String IAM_TOKEN;

    private final WebClient webClient;
    private final YandexProperties yandexProperties;
    private final HistoryRedisService historyRedisService;


    @Override
    public PartialBotApiMethod<? extends Serializable> newCall(Message inputMess) {

        String userChatId = String.valueOf(inputMess.getChatId());
        GptMessageUnit question = new GptMessageUnit(GptMessageUnit.Role.USER.getRole(), inputMess.getText());
        log.info("USER: {}", inputMess.getText());

        List<GptMessageUnit> history = historyRedisService.getHistory(userChatId);
        history.add(question);

        GptAnswer answer = this.buildRequest(history);

        GptMessageUnit answerUnit = answer.result().alternatives().getLast().message();

        CompletableFuture.runAsync(() -> {
            historyRedisService.addMessage(userChatId, question);
            historyRedisService.addMessage(userChatId, answerUnit);
        });
        log.info("ASSISTANT: {}", answerUnit.text());
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

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.HOURS) // Каждые 10 часов
    public void updateIamToken() {

        String iamToken = webClient.post()
                .uri(yandexProperties.getIamTokenUpdaterUrl())
                .bodyValue(new IamTokenRequest(yandexProperties.getOauthToken()))
                .retrieve()
                .bodyToMono(IamTokenResponse.class)
                .map(IamTokenResponse::getIamToken)
                .block();

        log.info("IamToken updated");
        IAM_TOKEN = iamToken;

    }

    @Getter
    @AllArgsConstructor
    static final class IamTokenRequest {
        @ToString.Exclude
        private final String yandexPassportOauthToken;
    }

    @Getter
    @ToString
    @AllArgsConstructor
    static final class IamTokenResponse {
        @ToString.Exclude
        private final String expiresAt;
        private final String iamToken;
    }

}
