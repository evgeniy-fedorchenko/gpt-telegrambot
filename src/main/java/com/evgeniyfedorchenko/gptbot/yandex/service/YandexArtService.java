package com.evgeniyfedorchenko.gptbot.yandex.service;

import com.evgeniyfedorchenko.gptbot.data.UserModeRedisService;
import com.evgeniyfedorchenko.gptbot.service.AiModelService;
import com.evgeniyfedorchenko.gptbot.telegram.Mode;
import com.evgeniyfedorchenko.gptbot.telegram.TelegramExecutor;
import com.evgeniyfedorchenko.gptbot.yandex.YandexProperties;
import com.evgeniyfedorchenko.gptbot.yandex.model.ArtAnswer;
import com.evgeniyfedorchenko.gptbot.yandex.model.ArtMessageUnit;
import com.evgeniyfedorchenko.gptbot.yandex.model.ArtRequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;


@Slf4j
@RequiredArgsConstructor
@Component(YandexArtService.SERVICE_NAME)
public class YandexArtService implements AiModelService {

    public static final String SERVICE_NAME = "YandexArtService";
    private double percentReady = 1;
    private Message process;


    private final YandexProperties yandexProperties;
    private final WebClient webClient;
    private final RetryTemplate retryTemplate;
    private final TelegramExecutor telegramExecutor;
    private final UserModeRedisService userModeRedisService;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Override
    public PartialBotApiMethod<? extends Serializable> newCall(Message inputMess) {

        String chatId = String.valueOf(inputMess.getChatId());
        userModeRedisService.setMode(chatId, Mode.YANDEX_ART_HOLDED);
        ArtAnswer artAnswer = requestGeneration(inputMess.getText());

        if (artAnswer.getId() != null) {
            process = telegramExecutor.sendAndReturn(new SendMessage(chatId, "Принято! Ожидаем завершения"));
            log.info("ART REQ: {}", inputMess.getText());
        } else {
            return new SendMessage(chatId, "Бля! Что-то пошло не так, давай по новой");
        }

        ArtAnswer answer = retryTemplate.execute(context -> webClient.get()
                .uri(yandexProperties.getArtModelCompleteUrlPattern().formatted(artAnswer.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + YandexGptService.IAM_TOKEN)
                .retrieve()
                .bodyToMono(ArtAnswer.class)
                .filter(resp -> ifDoneOrElseThrow(resp, chatId))
                .switchIfEmpty(Mono.error(() -> new RuntimeException("The picture is not ready yet")))
                .block());

        return completeGen(answer, chatId);
    }

    private SendPhoto completeGen(ArtAnswer answer, String chatId) {
        byte[] bytes = Base64.getDecoder().decode(answer.getResponse().image());
        InputFile result = new InputFile(new ByteArrayInputStream(bytes), "result");

        return new SendPhoto(chatId, result);
    }

    private ArtAnswer requestGeneration(String prompt) {
        ArtRequestBody body = ArtRequestBody.builder()
                .modelUri(yandexProperties.getArtModelUriPattern())
                .messages(Collections.singletonList(ArtMessageUnit.builder().text(prompt).build()))
                .build();

        return webClient.post()
                .uri(yandexProperties.getArtModelBaseUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + YandexGptService.IAM_TOKEN)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ArtAnswer.class)
                .block();
    }

    private Boolean ifDoneOrElseThrow(ArtAnswer maybeReady, String chatId) {

        if (maybeReady != null && maybeReady.isDone()) {
            CompletableFuture.runAsync(() -> {

                telegramExecutor.send(new DeleteMessage(chatId, process.getMessageId()));
                userModeRedisService.setMode(chatId, Mode.YANDEX_ART);
                percentReady = 1;
                log.info("ART RESP: done: {}, resp-not-null: {}", maybeReady.isDone(), maybeReady.getResponse() != null);

            }, taskExecutor);

            return true;

        } else {
            CompletableFuture.runAsync(() -> {
                OptionalDouble currentReadyOpt = calculatePercentReady(percentReady);
                String text;
                if (currentReadyOpt.isPresent()) {
                    text = "Генерация завершена на %.2f%%".formatted(currentReadyOpt.getAsDouble());
                    percentReady = currentReadyOpt.getAsDouble();
                } else {
                    text = "to de continue";
                }
                EditMessageText processMess = new EditMessageText(text);
                processMess.setChatId(chatId);
                processMess.setMessageId(process.getMessageId());

                telegramExecutor.send(processMess);

            }, taskExecutor);

            throw false
        }
    }

    private OptionalDouble calculatePercentReady(double current) {
        double logBase;

        if (current < 98.7) {

            if (current < 30) {
                logBase = 2;
            } else if (current < 60) {
                logBase = 2.5;
            } else if (current < 80) {
                logBase = 3;
            } else {
                logBase = 10;
            }
            return OptionalDouble.of(current + Math.log(100 - current) / Math.log(logBase));

        } else if (current < 99.37) {
            return OptionalDouble.of(current + (100 - current) / 5);

        } else if (current < 99.9) {
            return OptionalDouble.of(current + 0.01);

        } else {
            return OptionalDouble.empty();
        }
    }
}
