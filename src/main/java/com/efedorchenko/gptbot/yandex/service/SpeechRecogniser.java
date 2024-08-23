package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechRecogniser {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final YandexProperties yandexProperties;
    private final ExecutorService executorServiceOfVirtual;

    public Optional<String> recognize(byte[] bytes) {

        Request request = new Request.Builder()
                .url(yandexProperties.getRecognizeUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .post(RequestBody.create(bytes, okhttp3.MediaType.parse("multipart/form-data")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            String string = response.body().string();
            String result = objectMapper.readTree(string).get("result").asText();
            if (result.isEmpty()) {
                saveUnrecognizedVoiceAsync(bytes);
                return Optional.empty();
            }
            return Optional.of(result);

        } catch (IOException ex) {
            log.error("Ошибка распознавания. Ex: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void saveUnrecognizedVoiceAsync(byte[] bytes) {

        if (bytes.length == 0) {
            return;
        }

        CompletableFuture.runAsync(() -> {

            String mdcValue = MDC.get("RqUID") ;
            String fileNameWithoutExtension = mdcValue == null ? String.valueOf(LocalTime.now()) : mdcValue;
            String filePath = "logs/unrecognized_voices/" + LocalDate.now() + "/" + fileNameWithoutExtension + ".ogg";

            try {
                String dirPath = new File(filePath).getParent();
                if (dirPath != null) {
                    Path path = Paths.get(dirPath);
                    if (!Files.exists(path)) {
                        Files.createDirectories(path);
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(bytes);
                }

            } catch (IOException e) {
                log.error("Exception occurred when writing an unrecognized voice to a file: {}", e.getMessage());
            }

        }, executorServiceOfVirtual);
    }

}