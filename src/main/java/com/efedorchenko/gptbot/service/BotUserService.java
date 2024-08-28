package com.efedorchenko.gptbot.service;

import com.efedorchenko.gptbot.data.BotUserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class BotUserService {

    private static final String resetDailyMetricsTZ = "Europe/Moscow";

    private final BotUserRepository botUserRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = resetDailyMetricsTZ)   // Каждый день в полночь
    public void resetDailyMetrics() {
        botUserRepository.resetDailyMetrics();
        log.info("Daily metrics have been reset");
    }

}
