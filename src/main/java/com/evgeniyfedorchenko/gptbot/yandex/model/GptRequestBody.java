package com.evgeniyfedorchenko.gptbot.yandex.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Класс представляет собой тело запроса, который направляется на обработку модели {@code YandexGPT}
 */
@Getter
@Builder
@ToString
public class GptRequestBody implements Serializable {

    /**
     * Внутренний адрес модели Яндекса.
     * Параметр {@code a2gmcltgwefld9t36qwe} - идентификатор каталога Yandex Cloud
     *
     * @example {@code "gpt://a2gmcltgwefld9t36qwe/yandexgpt}
     */
    private final String modelUri;

    /**
     * Предпочтительные параметры конфигурации ответа: режим потока, температура, ограничение по токенам
     */
    @Builder.Default
    private final CompletionOptions completionOptions = CompletionOptions.builder().build();

    /**
     * История переписки с моделью, которую должна "помнить" модель для ведения диалога
     */
    private final List<GptMessageUnit> messages;

    /**
     * Технические настройки желаемого ответа
     */
    @Getter
    @Builder
    @ToString
    public static class CompletionOptions implements Serializable {

        /**
         * Режим потока - включает потоковую передачу частично сгенерированного текста.
         */
        @Builder.Default
        private final boolean stream = false;

        /**
         * Мера непредсказуемости/креативности. Чем выше значение этого параметра, тем более креативными и
         * случайными будут ответы модели. Принимает число от нуля до единицы, обе границы включительно
         */
        @Builder.Default
        private final double temperature = 0.5D;

        /**
         * Устанавливает ограничение на выход модели в токенах, подробнее см. <a href="https://yandex.cloud/ru/docs/foundation-models/concepts/limits">Квоты и лимиты в Yandex Foundation Models</a>
         */
        @Builder.Default
        private final int maxTokens = 2000;

    }
}
