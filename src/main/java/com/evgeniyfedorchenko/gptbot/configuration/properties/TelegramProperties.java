package com.evgeniyfedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(
        prefix = TelegramProperties.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class TelegramProperties {

    static final String CONFIGURATION_PREFIX = "telegram-bot";

    @NotBlank
    @Pattern(regexp = "^\\d{10}:[A-Za-z\\d_]{35}$")   // Regexp: 10 цифр, затем двоеточие и еще 35 латинских букв/цифр
    private String token;

    @NotBlank
    @Pattern(regexp = "^.{2,}bot$")   // Regexp: строка длиной 5 и более символов, заканчивается на "bot"
    private String username;
}
