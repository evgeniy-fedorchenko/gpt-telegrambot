package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.data.UserModeRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final UserModeRedisService userModeCache;
    public static final ThreadLocal<User> localUser = new ThreadLocal<>();


    public TelegramBot(@Value("${telegram-bot.token}") String botToken,
                       TelegramExecutor telegramExecutor,
                       ExecutorService executorServiceOfVirtual,
                       TelegramDistributor telegramDistributor,
                       UserModeRedisService userModeCache) {
        super(botToken);
        this.telegramExecutor = telegramExecutor;
        this.executorServiceOfVirtual = executorServiceOfVirtual;
        this.telegramDistributor = telegramDistributor;
        this.userModeCache = userModeCache;
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
                        if (userModeCache.getMode(update.getMessage().getChatId()).equals(Mode.YANDEX_ART_HOLDED)) {
                            userModeCache.setMode(update.getMessage().getChatId(), Mode.YANDEX_ART);
                        }
                        log.error("Processing has FAILED for updateID {}. Ex: ", update.getUpdateId(), ex);
                        return null;
                    });
        }
    }

}
