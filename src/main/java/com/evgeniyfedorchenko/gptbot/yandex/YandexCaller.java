package com.evgeniyfedorchenko.gptbot.yandex;

import com.evgeniyfedorchenko.gptbot.yandex.artmodels.ArtGptRequestBody;
import com.evgeniyfedorchenko.gptbot.yandex.artmodels.ArtMessage;
import com.evgeniyfedorchenko.gptbot.yandex.artmodels.GenerationOptions;
import com.evgeniyfedorchenko.gptbot.yandex.artmodels.GptArtAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.models.GptRequestBody;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
                .completionOptions(new GptRequestBody.CompletionOptions(false, 0.6D, 2000))
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

        GenerationOptions options = GenerationOptions.builder()
                .seed(9)
                .mimeType(MediaType.IMAGE_JPEG_VALUE)
                .aspectRatio(new GenerationOptions.AspectRatio(1, 1))
                .build();
        ArtGptRequestBody body = ArtGptRequestBody.builder()
                .modelUri(yandexProperties.getArtModelUriPattern().formatted(yandexProperties.getFolderId()))
                .generationOptions(options)
                .messages(Collections.singletonList(new ArtMessage(1, prompt)))
                .build();


        WebClient cleanWebClient = WebClient.builder().build();

        String imageId = cleanWebClient.post()
                .uri(yandexProperties.getArtModelBaseUrl().formatted(yandexProperties.getFolderId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GptArtAnswer.class)
                .block()

                .getId();


        return imageId;

    }


    public String checkImage(String imageId) {
        WebClient build = WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .baseUrl(yandexProperties.getArtModelCompleteUrlPattern().formatted(imageId))
                .build();
        GptArtAnswer answer = build.get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IAM_TOKEN)
                .retrieve()
                .bodyToMono(GptArtAnswer.class)
                .block();

        return answer.isDone()
                ? answer.getResponse().getImage()
                : "not ready";
//        return answer;
    }


}

/*

{
"modelUri": "art://<идентификатор_каталога>/yandex-art/latest",
"generationOptions": {
  "seed": "1863",
  "aspectRatio": {
     "widthRatio": "2",
     "heightRatio": "1"
   }
},
"messages": [
  {
    "weight": "1",
    "text": "узор из цветных пастельных суккулентов разных сортов, hd full wallpaper, четкий фокус, множество сложных деталей, глубина кадра, вид сверху"
  }
]
}

*/
