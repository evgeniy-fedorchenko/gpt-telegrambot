package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.configuration.properties.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramExecutor telegramExecutor;
    private final ExecutorService executorServiceOfVirtual;
    private final TelegramDistributor telegramDistributor;
    private final TelegramProperties telegramProperties;
    public static final ThreadLocal<User> localUser = new ThreadLocal<>();

    public TelegramBot(TelegramExecutor telegramExecutor,
                       ExecutorService executorServiceOfVirtual,
                       TelegramDistributor telegramDistributor,
                       TelegramProperties telegramProperties) {
        super(telegramProperties.getToken());
        this.telegramExecutor = telegramExecutor;
        this.executorServiceOfVirtual = executorServiceOfVirtual;
        this.telegramDistributor = telegramDistributor;
        this.telegramProperties = telegramProperties;
    }

    @Override
    public String getBotUsername() {
        return telegramProperties.getUsername();
    }

    /**
     * Точка входа в приложение со стороны Телеграм-бота.
     * Метод первично валидирует принятый объект, после чего направляет на маршрутизацию и обработку.
     * После обработки ответ направляется в {@link TelegramExecutor#send(PartialBotApiMethod)} для
     * отправки контента юзеру
     *
     * @param update корневой объект, содержащий всю информацию о пришедшем обновлении
     */
    @Override
    public void onUpdateReceived(Update update) {

        if (update != null) {

            localUser.set(update.getMessage().getFrom());

            CompletableFuture.supplyAsync(() -> {
                        log.debug("Processing has BEGUN for updateID {}", update.getUpdateId());
                        return telegramDistributor.distribute(update);

                    }, executorServiceOfVirtual)

                    .thenAccept(result -> {
                        telegramExecutor.send(result);
                        log.debug("Processing has FINISHED for updateID {}", update.getUpdateId());
                    })

                    .exceptionally(ex -> {
                        log.error("Processing has FAILED (NOT HANDLED) for updateID {}. Ex: ", update.getUpdateId(), ex);
                        return null;
                    });
        }
    }

}
