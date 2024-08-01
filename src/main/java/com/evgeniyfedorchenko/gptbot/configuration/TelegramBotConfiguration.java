package com.evgeniyfedorchenko.gptbot.configuration;

import com.evgeniyfedorchenko.gptbot.telegram.TelegramBot;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
@AllArgsConstructor
public class TelegramBotConfiguration {

    private final ApplicationContext applicationContext;

    /**
     * Бин взаимодействия с Telegram API
     * <p>
     * Настройка бина для контекста Spring на основе собранного объекта
     * {@link TelegramBot} и регистрирует его на серверах telegram
     * @param telegramBot Инициализированный объект для регистрации
     * @return Системный объект для работы с серверами Telegram
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) {

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBot);
            return telegramBotsApi;

        } catch (TelegramApiException ex) {
            log.error("Failed to register the Telegram-bot, фзз has been stopped running. Cause: ", ex);
            SpringApplication.exit(applicationContext);
            System.exit(1);
            return null;
        }
    }

}
