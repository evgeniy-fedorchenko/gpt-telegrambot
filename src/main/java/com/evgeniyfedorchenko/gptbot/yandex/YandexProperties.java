package com.evgeniyfedorchenko.gptbot.yandex;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(
        prefix = YandexProperties.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false
)
public class YandexProperties {

    static final String CONFIGURATION_PREFIX = "yandex";

    private String baseUrl;

    private String folderId;

    private String oauthToken;

    private String modelUriPattern;

    private String iamTokenUpdaterUrl;

}
