package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.telegram.TelegramBot;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.ThreadFactory;

@Component
public class VirtualThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(@NotNull Runnable r) {

        String rquid = MDC.get("RqUID");
        if (rquid == null) {
            User user = TelegramBot.localUser.get();
            rquid = user == null ? "system" : System.currentTimeMillis() + "-" + user.getId();
        }

        String finalRquid = rquid;
        Runnable decorated = () -> {
            MDC.put("RqUID", finalRquid);
            r.run();
        };

        return Thread.ofVirtual().unstarted(decorated);

    }
}
