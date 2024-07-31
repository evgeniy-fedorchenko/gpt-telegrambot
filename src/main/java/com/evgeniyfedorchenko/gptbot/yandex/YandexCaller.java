package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.yandex.models.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptRequestBody;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
    public GptAnswer buildRequest(List<GptMessageUnit> history) {

        return webClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .bodyValue(buildBody(history))
                .retrieve()
                .bodyToMono(GptAnswer.class)
                .block();
    }

    private GptRequestBody buildBody(List<GptMessageUnit> history) {

        return GptRequestBody.builder()
                .modelUri(yandexProperties.getModelUriPattern().formatted(yandexProperties.getFolderId()))
                .completionOptions(new GptRequestBody.CompletionOptions(false, 0.6D, 2000))
                .gptMessageUnits(history)
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
