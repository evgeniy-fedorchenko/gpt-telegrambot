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

    @NotBlank
    private String chatbotBaseUrl;

    @NotBlank
    private String artModelBaseUrl;

    @NotBlank
    private String chatbotUriPattern;

    @NotBlank
    private String artModelUriPattern;

    @NotEmpty
    @URL(protocol = "https")
    private String artModelCompleteUrlPattern;

    @NotEmpty
    @URL(protocol = "https")
    private String iamTokenUpdaterUrl;

    @NotEmpty
    @Pattern(regexp = "^[a-z0-9]{15,25}$")
    private String folderId;

    @NotEmpty
    @Pattern(regexp = "^y[0-3]_[a-zA-Z0-9_-]{58}$")
    private String oauthToken;

    @PostConstruct
    public void yandexPropertiesFormatted() {
        this.chatbotUriPattern = this.chatbotUriPattern.formatted(folderId);
        this.artModelUriPattern = this.artModelUriPattern.formatted(folderId);
    }
}
