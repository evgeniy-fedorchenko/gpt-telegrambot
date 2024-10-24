package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.OkHttpClientConfiguration;
import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Async("executorOfVirtual")
public class IamTokenSupplier {

    public static String IAM_TOKEN;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final YandexProperties yandexProperties;

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.HOURS) // Каждые 10 часов
    public void updateIamToken() {

        IamTokenResponse iamTokenResponse = this.executeLater(0L).orElseGet(() -> {

            log.warn("Cannot update IAM-token on the first try, try again...");
            Optional<IamTokenResponse> iamTokenResponseOpt = executeLater(5_000L);

            if (iamTokenResponseOpt.isEmpty()) {
                log.error("Cannot update IAM-token on the second try, skipped");
                return null;
            }
            return iamTokenResponseOpt.get();
        });

        if (iamTokenResponse != null) {
            IAM_TOKEN = iamTokenResponse.getIamToken();
            log.debug("IamToken has been updated");
        }
    }

    private Optional<IamTokenResponse> executeLater(long waitMillis) {
        try {
            Thread.sleep(waitMillis);
            String body = objectMapper.writeValueAsString(new IamTokenRequest(yandexProperties.getOauthToken()));

            Request request = new Request.Builder()
                    .url(yandexProperties.getIamTokenUpdaterUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(YandexProperties.FOLDER_ID_HEADER_NAME, yandexProperties.getFolderId())
                    .post(RequestBody.create(body, OkHttpClientConfiguration.MT_APPLICATION_JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful() && response.body() != null
                        ? Optional.of(objectMapper.readValue(response.body().string(), IamTokenResponse.class))
                        : Optional.empty();
            }
        } catch (IOException | InterruptedException ex) {
            log.error("IAM token update failed. Ex: ", ex);
            return Optional.empty();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class IamTokenRequest {
        private final String yandexPassportOauthToken;
    }

    @Getter
    @ToString
    @AllArgsConstructor
    private static class IamTokenResponse {
        private final String expiresAt;
        @ToString.Exclude
        private final String iamToken;
    }

}
