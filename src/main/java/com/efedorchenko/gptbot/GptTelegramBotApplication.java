package com.efedorchenko.gptbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GptTelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(GptTelegramBotApplication.class, args);
	}

	// TODO 10.08.2024 16:22: генерация картинки асинхронно для юзера (на беке увеличивать промежуток между временем опроса)
	// TODO 10.08.2024 16:23: admin-api для получения статистики (уточнить эндпоинты)
	// TODO 10.08.2024 16:24: добавление Midjourney
	// TODO 10.08.2024 16:26: добавление ChatGPT
	// TODO 12.08.2024 21:40: логирование для команд
	// TODO 12.08.2024 21:59: разделить профили на тест и прод. Завести отдельные базу, кеш, бота - для разделения использовать профили Spring + application.properties
	// TODO 13.08.2024 00:57: отладить аспект логирования
	// TODO 13.08.2024 22:34: сделать StatsController (получить статистику из бд) и SendContentController (отправить сообщение юзерам)
	// TODO 14.08.2024 10:48 - собрать прилагу в докере
	// TODO 14.08.2024 23:22: ввести маркеры в логирование исключений в MethodLogAspect
	// TODO 15.08.2024 23:36: в логерах проверять, что есть итерабл, то только последний элемент логировать (или пару-тройку)
	// TODO 15.08.2024 23:38: в логерах создать список классов (или мб полей для игнора) и проверять их перед логированием
	// TODO 16.08.2024 11:05 - при получении апдейта: если нет текста то дать ответ юзеру
}
