package com.evgeniyfedorchenko.gptbot.configuration.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Component
@Validated
@ConfigurationProperties(
        prefix = YandexProperties.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class YandexProperties {

    static final String CONFIGURATION_PREFIX = "yandex";

    /**
     * Урл, по которому можно обратиться к модели {@code YandexGPT} через http (синхронный доступ)
     */
    @NotBlank
    @URL(protocol = "https")
    private String chatbotBaseUrl;

    /**
     * Урл, по которому можно обратиться к модели {@code YandexART} через http (асинхронный доступ)
     */
    @NotBlank
    @URL(protocol = "https")
    private String artModelBaseUrl;

    /**
     * Внутренний URI. Идентификатор модели {@code YandexGPT}, содержащий текстовый
     * плейсхолдер для вставки идентификатор каталога через {@link String#formatted}
     */
    @NotBlank
    private String chatbotUriPattern;

    /**
     * Внутренний URI. Идентификатор модели {@code YandexART}, содержащий текстовый
     * плейсхолдер для вставки идентификатор каталога через {@link String#formatted}
     */
    @NotBlank
    private String artModelUriPattern;

    /**
     * Адрес для проверки готовности изображения, отправленного на генерацию к {@code YandexART},
     * содержащий текстовый плейсхолдер для вставки идентификатор каталога через {@link String#formatted}
     */
    @NotEmpty
    @URL(protocol = "https")
    private String artModelCompleteUrlPattern;

    /**
     * Урл для обновления IAM-токена
     */
    @NotEmpty
    @URL(protocol = "https")
    private String iamTokenUpdaterUrl;

    /**
     * Идентификатор задействованного каталога Yandex Cloud.<br>
     * Regexp: СТрока длинной от 15 до 25 (границы вкл) строчных латинских букв или цифр
     */
    @NotEmpty
    @Pattern(regexp = "^[a-z0-9]{15,25}$")
    private String folderId;

    /**
     * Главный токен авторизации в системах Foundation Models на Yandex Cloud.
     * Используется для получения и обновления IAM-токена.<br>
     * Regexp: Латинская строчная "Y", затем цифра от [0 - 3], затем 58 латинских букв/цифр,
     * допустим символ "Hyphen-minus", hex в UTF-8: U+002D
     */
    @NotEmpty
    @Pattern(regexp = "^y[0-3]_[a-zA-Z0-9_-]{58}$")
    private String oauthToken;

    @PostConstruct
    public void yandexPropertiesFormatted() {
        this.chatbotUriPattern = this.chatbotUriPattern.formatted(folderId);
        this.artModelUriPattern = this.artModelUriPattern.formatted(folderId);
    }
}
