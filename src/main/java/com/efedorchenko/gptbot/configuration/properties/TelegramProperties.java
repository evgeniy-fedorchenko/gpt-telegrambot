package com.efedorchenko.gptbot.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@AllArgsConstructor(onConstructor_ = @ConstructorBinding)
@ConfigurationProperties(prefix = TelegramProperties.CONFIGURATION_PREFIX, ignoreUnknownFields = false)
public class TelegramProperties {

    static final String CONFIGURATION_PREFIX = "telegram-bot";

    /**
     * Уникальный токен для доступа и управления ботом<br>
     * Regexp: 10 цифр, затем двоеточие, и 35 латинских букв/цифр (допустим символ "Low Line", hex UTF-8: U+005F)
     */
    @NotBlank
    @Pattern(regexp = "^\\d{10}:[A-Za-z\\d_]{35}$")
    private final String token;

    /**
     * Уникальный username бота (не путать с именем)<br>
     * Regexp: строка длиной 5 и более символов, заканчивается на "bot"
     */
    @NotBlank
    @Pattern(regexp = "^.{2,}bot$")
    private final String username;

}
