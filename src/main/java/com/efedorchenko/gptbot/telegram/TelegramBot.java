package com.efedorchenko.gptbot.telegram;

import com.efedorchenko.gptbot.configuration.properties.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    /* Spring java beans */
    private final TelegramExecutor telegramExecutor;
    private final ExecutorService executorServiceOfVirtual;
    private final TelegramDistributor telegramDistributor;
    private final TelegramProperties telegramProperties;

    /* Message's patterns for logging main stages processing update */
    private static final String BEGUN = "Processing has BEGUN for updateID {}";
    private static final String FINISHED_NORMALLY = "Processing has FINISHED normally for updateID {}";
    private static final String FINISHED_NORMALLY_NULL = "Processing has FINISHED (null specially, not sent anything) for updateID {}";
    private static final String FILED = "Processing has FAILED (NOT HANDLED) for updateID {}. Ex: ";

    /* Other fields (or constants) */
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

        if (update != null && update.hasMessage()) {

            localUser.set(update.getMessage().getFrom());

            CompletableFuture.supplyAsync(() -> {
                        log.debug(BEGUN, update.getUpdateId());
                        return Optional.ofNullable(telegramDistributor.distribute(update));

                    }, executorServiceOfVirtual)

                    .thenAccept(resultOpt -> resultOpt.ifPresentOrElse(result -> {
                                telegramExecutor.send(result);
                                log.debug(FINISHED_NORMALLY, update.getUpdateId());

                            }, () -> log.debug(FINISHED_NORMALLY_NULL, update.getUpdateId()))

                    ).exceptionally(ex -> {
                        log.error(FILED, update.getUpdateId(), ex);
                        return null;
                    });

        } else {
            log.warn("Received unknown update: {}", update);
        }
    }

}
