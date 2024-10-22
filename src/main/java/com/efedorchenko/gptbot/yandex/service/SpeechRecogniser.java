package com.efedorchenko.gptbot.yandex.service;

import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import com.efedorchenko.gptbot.yandex.model.SpeechKitAnswer;
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

import static com.efedorchenko.gptbot.utils.logging.LogUtils.LOGIC_MARKER;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechRecogniser {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorOfVirtual;
    private final YandexProperties yandexProperties;

    public Optional<SpeechKitAnswer> doRecognize(byte[] bytes) {

        Request request = new Request.Builder()
                .url(yandexProperties.getRecognizeUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + IamTokenSupplier.IAM_TOKEN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(YandexProperties.FOLDER_ID_HEADER_NAME, yandexProperties.getFolderId())
                .post(RequestBody.create(bytes, okhttp3.MediaType.parse("multipart/form-data")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            String string = response.body().string();
            SpeechKitAnswer recAnswer = objectMapper.readValue(string, SpeechKitAnswer.class);

//            Возвращается сам объект, чтобы выше достать errorMessage
            if (recAnswer.getResult() == null) {
                return Optional.of(recAnswer);
            }
            if (recAnswer.getResult().isEmpty()) {
                log.warn("Unrecognized voice message has been detected, Saving");
                saveUnrecognizedVoiceAsync(bytes);
                return Optional.empty();
            }
            return Optional.of(recAnswer);

        } catch (IOException ex) {
            log.error("Unexpected http-response from the SpeechKit network. Ex: {}", ex.getMessage());
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
                    log.debug("Unrecognized voice message has been saved");
                }

            } catch (IOException e) {
                log.error(LOGIC_MARKER, "Exception occurred when writing an unrecognized voice to a file: {}", e.getMessage());
            }

        }, executorOfVirtual);
    }

}
