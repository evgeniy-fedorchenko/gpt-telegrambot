package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class SpeechRecogniser {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final YandexProperties yandexProperties;
    public String recognize(byte[] bytes) {

        Request request = new Request.Builder()
                .url(yandexProperties.getRecognizeUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .post(RequestBody.create(bytes, okhttp3.MediaType.parse("multipart/form-data")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String string = response.body().string();
            return objectMapper.readTree(string).get("result").asText();
        } catch (IOException ex) {
            throw new RuntimeException("Ошибка распознавания. Ex: " + ex); // FIXME 19.08.2024 21:46
        }
    }
}
