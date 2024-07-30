package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.yandex.models.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptRequestBody;
import com.evgeniyfedorchenko.gptbot.yandex.models.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class YandexCaller {

    private static String IAM_TOKEN;
    private final YandexProperties yandexProperties;
    private final WebClient webClient;

    @SneakyThrows
    public String buildRequest(String inputText) {

        GptRequestBody body = buildBody(inputText);
        String s = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body);
        GptAnswer response = webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GptAnswer.class)
                .block();

        return response.result().alternatives().getFirst().message().text();
    }

    private GptRequestBody buildBody(String inputText) {
        List<Message> messages = new ArrayList<>(List.of(
                new Message(Message.Role.USER.getRole(), inputText)
        ));
        return GptRequestBody.builder()
                .modelUri(yandexProperties.getModelUriPattern().formatted(yandexProperties.getFolderId()))
                .completionOptions(new GptRequestBody.CompletionOptions(false, 0.6D, 2000))
                .messages(messages)
                .build();
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.HOURS) // Каждые 10 часов
    public void updateIamToken() {

        String iamToken = webClient.post()
                .uri(yandexProperties.getIamTokenUpdaterUrl())
                .bodyValue(new IamTokenRequest(yandexProperties.getOauthToken()))
                .retrieve()
                .bodyToMono(IamTokenResponse.class)
                .map(IamTokenResponse::iamToken)
                .block();

        log.info("IamToken updated");
        IAM_TOKEN = iamToken;

    }

    record IamTokenRequest(String yandexPassportOauthToken) { }

    record IamTokenResponse(String iamToken, String expiresAt) { }

}
