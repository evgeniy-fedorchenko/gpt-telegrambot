package com.evgeniyfedorchenko.gptbot.configuration;

import com.evgeniyfedorchenko.gptbot.yandex.YandexProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

    @Bean
    public WebClient webClient(YandexProperties yandexProperties) {
        return WebClient.builder()
                .baseUrl(yandexProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-folder-id", yandexProperties.getFolderId())
                .build();
    }
}
