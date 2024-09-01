package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.telegram.TelegramBot;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.UUID;
import java.util.concurrent.ThreadFactory;

@Component
public class VirtualThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(@NotNull Runnable r) {

        // TODO 01.09.2024 22:29: Избавиться от LocalThread, использовать MDC, переносить весь контекст

        String rquid = MDC.get("RqUID");
        if (rquid == null) {
            User user = TelegramBot.localUser.get();
            String shortUuid = UUID.randomUUID().toString().substring(0, 14);
            rquid = shortUuid + (user == null ? "system" : user.getId());
        }

        final String finalRquid = rquid;
        Runnable decorated = () -> {
            MDC.put("RqUID", finalRquid);
            r.run();
        };

        return Thread.ofVirtual().unstarted(decorated);

    }
}
