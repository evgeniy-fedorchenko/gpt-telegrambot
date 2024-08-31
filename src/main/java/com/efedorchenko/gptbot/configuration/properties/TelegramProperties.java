package com.efedorchenko.gptbot.configuration.properties;

import com.efedorchenko.gptbot.telegram.TelegramDistributor;
import com.efedorchenko.gptbot.telegram.TelegramExecutor;
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
     * Регулярное выражение для валидации ссылки на канал. Классическая ссылка {@code https://...}
     * не предусмотрена, так как целевой объект, для которого создано свойство, принимает ссылку
     * только в таком формате, через {@code @}
     * <p>
     * Regexp: Первый символ - @ (символ "Commercial At" hex UTF-8: U+0040), далее еще
     * от 5 до 32 символов (обе границы вкл) - разрешен латинский алфавит, цифры и нижнее
     * подчеркивание "_" (символ "Low Line", hex UTF-8: U+005F). Но второй символ
     * (тот, что после "@") только кириллица, а последний только кириллица или цифра.<br>
     * Например: @Example_LINK777
     *
     * @see TelegramDistributor#checkSubscribes(long)
     * @see TelegramExecutor#checkSubscribesPositive(long, String)
     */
    private static final String CHANNEL_LINK_REGEX = "^@(?=.{5,32}$)[a-zA-Z][a-zA-Z0-9_]*[a-zA-Z0-9]$";

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

    /**
     * Один из двух каналов, на которые бот будет требовать подписаться, чтобы можно было этого бота использовать.
     * Второй канал - {@link TelegramProperties#accessChannelChildren}
     * @see TelegramProperties#accessChannelChildren
     */
    @NotBlank
    @Pattern(regexp = CHANNEL_LINK_REGEX)
    private final String accessChannelAdults;

    /**
     * Один из двух каналов, на которые бот будет требовать подписаться, чтобы можно было этого бота использовать
     * Второй канал - {@link TelegramProperties#accessChannelAdults}
     * @see TelegramProperties#accessChannelAdults
     */
    @NotBlank
    @Pattern(regexp = CHANNEL_LINK_REGEX)
    private final String accessChannelChildren;

}
