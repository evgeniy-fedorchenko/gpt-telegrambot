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

}
