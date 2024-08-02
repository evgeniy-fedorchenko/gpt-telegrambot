package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.yandex.model.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class YandexCaller {

    private static String IAM_TOKEN = "t1.9euelZqRmciYipuLioyTi8zHmYucyO3rnpWalJTJyovJisaPnI-elZmWj5zl9PdiKFFK-e8XVkaR3fT3IldOSvnvF1ZGkc3n9euelZqJkYuPiZCenY2RncacmIuRme_8xeuelZqJkYuPiZCenY2RncacmIuRmQ.aFSl82HoKGkJP63u69BaT274zKK903hpB-Tv_XOGRH-0xrEZEWbsl7tuTu3Zy45C6fqGwizBfbkRmOzDRjshAQ";
    private final YandexProperties yandexProperties;
    private final WebClient webClient;

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
                .modelUri(yandexProperties.getChatbotUriPattern().formatted(yandexProperties.getFolderId()))
                .messages(history)
                .build();
    }

//    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.HOURS) // Каждые 10 часов
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

    public String buildRequestArt(String prompt) {


        ArtRequestBody body = ArtRequestBody.builder()
                .modelUri(yandexProperties.getArtModelUriPattern().formatted(yandexProperties.getFolderId()))
                .messages(Collections.singletonList(ArtMessageUnit.builder().text(prompt).build()))
                .build();


        WebClient cleanWebClient = WebClient.builder().build();

        String imageId = cleanWebClient.post()
                .uri(yandexProperties.getArtModelBaseUrl().formatted(yandexProperties.getFolderId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ArtAnswer.class)
                .block()

                .id();


        return imageId;

    }


    public String checkImage(String imageId) {
        WebClient build = WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .baseUrl(yandexProperties.getArtModelCompleteUrlPattern().formatted(imageId))
                .build();
        ArtAnswer answer = build.get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .retrieve()
                .bodyToMono(ArtAnswer.class)
                .block();

        return answer.done()
                ? answer.response().image()
                : "not ready";
//        return answer;
    }


}
