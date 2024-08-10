package com.evgeniyfedorchenko.gptbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class GptTelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(GptTelegramBotApplication.class, args);
	}

	// TODO 10.08.2024 16:22: кнопка для очистки истории и начала нового чата
	// TODO 10.08.2024 16:22: генерация картинки асинхронно для юзера
	// TODO 10.08.2024 16:23: сбор статистики в интерцепторе
	// TODO 10.08.2024 16:23: admin-api для получения статистики (уточнить эндпоинты)
	// TODO 10.08.2024 16:24: добавление Midjourney
	// TODO 10.08.2024 16:26: добавление ChatGPT
	// TODO 10.08.2024 16:54: поставить лимиты на запросы
}
