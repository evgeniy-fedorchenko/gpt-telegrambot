package com.evgeniyfedorchenko.gptbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GptTelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(GptTelegramBotApplication.class, args);
	}

	// TODO 10.08.2024 16:22: генерация картинки асинхронно для юзера
	// TODO 10.08.2024 16:23: admin-api для получения статистики (уточнить эндпоинты)
	// TODO 10.08.2024 16:24: добавление Midjourney
	// TODO 10.08.2024 16:26: добавление ChatGPT
	// TODO 10.08.2024 16:54: поставить лимиты на поля запросов
	// TODO 12.08.2024 21:40: логирование для команд
	// TODO 12.08.2024 21:40: логирование по сигнатурам
	// TODO 12.08.2024 21:41: javadoc для основных методов, полей и классов
	// TODO 12.08.2024 21:44: улучшить обработку исключений и обратная связь юзеру, если оно было брошено
	// TODO 12.08.2024 21:59: разделить профили на тест и прод. Завести отдельные базу, кеш, бота - для разделения использовать профили Spring + application.properties
	// TODO 13.08.2024 00:57: отладить аспект логирования
	// TODO 13.08.2024 00:58: сделать все объекты Serializable
}
