package com.efedorchenko.gptbot.configuration.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@AllArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = YandexProperties.CONFIGURATION_PREFIX, ignoreUnknownFields = false)
public class YandexProperties {

    static final String CONFIGURATION_PREFIX = "yandex";

    /**
     * URL, по которому можно обратиться к модели {@code YandexGPT} через http (синхронный доступ)
     */
    @NotBlank
    @URL(protocol = "https")
    private final String chatbotBaseUrl;

    /**
     * URL, по которому можно обратиться к модели {@code YandexART} через http (асинхронный доступ)
     */
    @NotBlank
    @URL(protocol = "https")
    private final String artModelBaseUrl;

    /**
     * URL для обновления IAM-токена
     */
    @NotEmpty
    @URL(protocol = "https")
    private final String iamTokenUpdaterUrl;

    /**
     * Внутренний URI. Идентификатор модели {@code YandexGPT}, содержащий текстовый
     * плейсхолдер для вставки идентификатора каталога через {@link String#formatted}
     */
    @NotBlank
    private final String chatbotUri;

    /**
     * Внутренний URI. Идентификатор модели {@code YandexART}, содержащий текстовый
     * плейсхолдер для вставки идентификатора каталога через {@link String#formatted}
     */
    @NotBlank
    private final String artModelUri;

    /**
     * Адрес для проверки готовности изображения, отправленного на генерацию к {@code YandexART},
     * содержащий текстовый плейсхолдер для вставки идентификатор каталога через {@link String#formatted}
     */
    @NotEmpty
    @URL(protocol = "https")
    private final String artModelCompleteUrlPattern;

    /**
     * Идентификатор задействованного каталога Yandex Cloud.<br>
     * Regexp: СТрока длинной от 15 до 25 (границы вкл) строчных латинских букв или цифр
     */
    @NotEmpty
    @Pattern(regexp = "^[a-z0-9]{15,25}$")
    private final String folderId;

    /**
     * Главный токен авторизации в системах Foundation Models на Yandex Cloud.
     * Используется для получения и обновления IAM-токена.<p>
     * <b>Regexp:</b> Латинская строчная "Y", затем цифра в промежутке [0 - 3],
     * затем 58 латинских букв/цифр, допустим символ "Hyphen-minus", hex в UTF-8: U+002D
     */
    @NotEmpty
    @Pattern(regexp = "^y[0-3]_[a-zA-Z0-9_-]{58}$")
    private final String oauthToken;

    @NotEmpty
    @URL(protocol = "https")
    private final String recognizeUrl;

//    @NotEmpty
//    @URL(protocol = "https")
//    private final String synthesizeUrlPattern;

    @PostConstruct
    public void yandexPropertiesFormatted() {
//        this.chatbotUriPattern = this.chatbotUriPattern.formatted(folderId);
//        this.artModelUriPattern = this.artModelUriPattern.formatted(folderId);
//        this.recognizeUrlPattern = this.recognizeUrlPattern.formatted(folderId);
//        this.synthesizeUrlPattern = this.synthesizeUrlPattern.formatted(folderId);
    }
}
