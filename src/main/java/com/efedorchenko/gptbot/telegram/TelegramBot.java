package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.configuration.properties.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramProperties telegramProperties;
    private final TelegramUpdateHandler telegramUpdateHandler;
    private final ExecutorService executorServiceOfVirtual;

    public TelegramBot(TelegramProperties telegramProperties,
                       TelegramUpdateHandler telegramUpdateHandler,
                       ExecutorService executorServiceOfVirtual) {
        super(telegramProperties.getToken());
        this.telegramProperties = telegramProperties;
        this.telegramUpdateHandler = telegramUpdateHandler;
        this.executorServiceOfVirtual = executorServiceOfVirtual;
    }

    @Override
    public String getBotUsername() {
        return telegramProperties.getUsername();
    }

    /**
     * Точка входа в приложение со стороны Телеграм-бота.
     * <p>
     * Метод первично валидирует принятый объект, после чего направляет на маршрутизацию и обработку. После обработки,
     * если результат не {@code null} - ответ направляется  в {@link TelegramExecutor#send(PartialBotApiMethod)} для
     * отправки контента юзеру. Если результат, возвращенный обработчиком {@link TelegramDistributor#distribute(Update)}
     * равен {@code null} - то он будет проигнорирован.
     * Так же метод отдельно логирует основные стадии работы бота - ПРИЕМ апдейта, ОБРАБОТКА и ОТПРАВКА
     *
     * @param update корневой объект, содержащий всю информацию о пришедшем обновлении
     */
    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> telegramUpdateHandler.handleUpdate(update), executorServiceOfVirtual);
    }

}
