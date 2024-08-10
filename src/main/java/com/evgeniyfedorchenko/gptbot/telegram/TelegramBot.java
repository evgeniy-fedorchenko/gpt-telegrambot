package com.evgeniyfedorchenko.gptbot.telegram;

import com.evgeniyfedorchenko.gptbot.configuration.properties.TelegramProperties;
import com.evgeniyfedorchenko.gptbot.data.UserModeRedisService;
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
    private final UserModeRedisService userModeCache;
    private final TelegramProperties telegramProperties;
    public static final ThreadLocal<User> localUser = new ThreadLocal<>();

    public TelegramBot(TelegramExecutor telegramExecutor,
                       ExecutorService executorServiceOfVirtual,
                       TelegramDistributor telegramDistributor,
                       UserModeRedisService userModeCache,
                       TelegramProperties telegramProperties) {
        super(telegramProperties.getToken());
        this.telegramExecutor = telegramExecutor;
        this.executorServiceOfVirtual = executorServiceOfVirtual;
        this.telegramDistributor = telegramDistributor;
        this.userModeCache = userModeCache;
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
                        if (userModeCache.getMode(update.getMessage().getChatId()).equals(Mode.YANDEX_ART_HOLDED)) {
                            userModeCache.setMode(update.getMessage().getChatId(), Mode.YANDEX_ART);
                        }
                        log.error("Processing has FAILED for updateID {}. Ex: ", update.getUpdateId(), ex);
                        return null;
                    });
        }
    }

}
