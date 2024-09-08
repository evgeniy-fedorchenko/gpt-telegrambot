package com.efedorchenko.gptbot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static com.efedorchenko.gptbot.utils.logging.LogUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    /* Spring java beans */
    private final TelegramExecutor telegramExecutor;
    private final TelegramDistributor telegramDistributor;

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
                log.error(LOGIC_MARKER, FILED_UNHANDLED, update.getUpdateId(), t);
            }

        } else {
            log.warn("Received unknown update: {}", update);
        }
    }

}
