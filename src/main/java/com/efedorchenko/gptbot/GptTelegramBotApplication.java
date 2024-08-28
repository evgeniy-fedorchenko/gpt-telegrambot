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

}
