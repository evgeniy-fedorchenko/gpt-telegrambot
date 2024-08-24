package com.efedorchenko.gptbot;

import com.efedorchenko.gptbot.configuration.properties.YandexProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(YandexProperties.class)
@SpringBootApplication
public class GptTelegramBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(GptTelegramBotApplication.class, args);
	}

	// ----- Release 1.0.0 -----

	/*
	  TODO Release 2.0.0:
	    1. Переезд на HttpClient от Apache
	    2. Логирование:
	    	2.1 Адаптировать HttpLogInterceptor под новый HttpClient
	    	2.2 logback-spring.xml: создать паттерны и апендеры для: console, app(совмещено со spring), exception
	    	2.3 logback-spring.xml: добавить поддержку маркеров для логирования
	    	2.4 создать лейауты для маскирования конфиденциальных данных на уровне формирования лог-сообщений (паттерн)
	    	2.5 подумать: исключать из лог-сообщений длинные iterable-последовательности
	    3. переезд чатбота на ChatGPT от OpenAI
	    4. переезд генератора изображений на Midjourney/Stable Diffusion/Dall-E
	    5. Генерация изображения асинхронно для юзера
	    6. Распространение бота (по необходимости)
	    7. Рефакторинг (по необходимости)
	    8. Подключение платежей к боту
	    9. SendContentController (отправить сообщение юзерам)
	   10. Подумать:
	       10.1 расширение ведения статистики (добавить поля)
	       10.2 admin-api для получения статистики
	       10.3 переезд в vps (webhook)

	*/

}
