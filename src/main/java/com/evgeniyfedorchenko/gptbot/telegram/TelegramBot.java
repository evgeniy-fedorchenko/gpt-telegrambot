package com.evgeniyfedorchenko.gptbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramExecutor telegramExecutor;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final TelegramDistributor telegramDistributor;

    public TelegramBot(@Value("${telegram-bot.token}") String botToken,
                       TelegramExecutor telegramExecutor,
                       @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
                       TelegramDistributor telegramDistributor) {
        super(botToken);
        this.telegramExecutor = telegramExecutor;
        this.taskExecutor = taskExecutor;
        this.telegramDistributor = telegramDistributor;
    }

    @Override
    public String getBotUsername() {
        return "gpt_exp_bot";
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

            log.debug("Processing has BEGUN for updateID {}", update.getUpdateId());

            CompletableFuture.supplyAsync(() -> telegramDistributor.distribute(update), taskExecutor)
                    .thenAccept(result -> {
                        telegramExecutor.send(result);
                        log.debug("Processing has FINISHED for updateID {}", update.getUpdateId());
                    })

                    .exceptionally(ex -> {
                        log.error("Processing has FAILED for updateID {}", update.getUpdateId(), ex);
                        log.error("ex: ", ex.getCause());
                        return null;
                    });
        }
    }

}
