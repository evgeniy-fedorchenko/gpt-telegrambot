package com.efedorchenko.gptbot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    /* Spring java beans */
    private final TelegramExecutor telegramExecutor;
    private final TelegramDistributor telegramDistributor;

    /* Message's patterns for logging main stages processing update */
    private static final String BEGUN = "Processing has BEGUN for updateID {}";
    private static final String FINISHED_NORMALLY = "Processing has FINISHED normally for updateID {}";
    private static final String FINISHED_NORMALLY_NULL = "Processing has FINISHED (null specially, not sent anything) for updateID {}";
    private static final String FILED_UNHANDLED = "Processing has FAILED (NOT HANDLED) for updateID {}. Ex: ";

    /* Other fields (or constants) */
    public static final ThreadLocal<User> localUser = new ThreadLocal<>();

    public void handleUpdate(Update update) {

        if (update != null && update.hasMessage()) {

            try {
                log.debug(BEGUN, update.getUpdateId());
                Optional.ofNullable(telegramDistributor.distribute(update))
                        .ifPresentOrElse(distributed -> {
                            telegramExecutor.send(distributed);
                            log.debug(FINISHED_NORMALLY, update.getUpdateId());

                        }, () -> log.debug(FINISHED_NORMALLY_NULL, update.getUpdateId()));

            } catch (Throwable t) {
                log.error(FILED_UNHANDLED, update.getUpdateId(), t);
            }

        } else {
            log.warn("Received unknown update: {}", update);
        }
    }

}
