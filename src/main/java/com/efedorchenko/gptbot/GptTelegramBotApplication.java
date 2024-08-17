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

	// TODO 14.08.2024 23:22: ввести маркеры в логирование исключений в MethodLogAspect и logback-spring.xml
	// TODO 18.08.2024 03:12: отрефакторить logback-spring.xml
	// TODO 15.08.2024 23:36: в логерах проверять, что есть итерабл, то только последний элемент логировать (или пару-тройку) - слишком долго
	// TODO 17.08.2024 15:30: подключить гс

	// ----- Release 1.0.0 -----
	// TODO 10.08.2024 16:22: генерация картинки асинхронно для юзера (на беке увеличивать промежуток между временем опроса)
	// TODO 10.08.2024 16:24: добавление Midjourney
	// TODO 10.08.2024 16:26: добавление ChatGPT

	// TODO 10.08.2024 16:23: admin-api для получения статистики (уточнить эндпоинты)
	// TODO 13.08.2024 22:34: сделать StatsController (получить статистику из бд) и SendContentController (отправить сообщение юзерам)
}
