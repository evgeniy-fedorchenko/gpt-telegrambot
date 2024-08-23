package com.efedorchenko.gptbot.yandex.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.MediaType;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 * Класс представляет собой тело запроса, который направляется на обработку модели {@code YandexART}
 */
@Getter
@Builder
@ToString
public class ArtRequestBody implements Serializable {

    /**
     * Внутренний идентификатор модели {@code YandexART}, содержащий идентификатор каталога Yandex Cloud.
     * Параметр {@code a2gmcltgwefld9t36qwe} - это идентификатор каталога Yandex Cloud
     *
     * @example {@code "gpt://a2gmcltgwefld9t36qwe/yandex-art/latest}
     */
    private final String modelUri;

    /**
     * Класс представляет один объект сообщения, отправляемого модели {@code YandexART} для генерации.
     * При множетстве описаний (объектов {@link ArtMessageUnit}) итоговое изображение будет в любом случае
     * только одно, рассчитанное из соотношения сторон
     */
    private final List<ArtMessageUnit> messages;

    /**
     * Предпочтительные параметры конфигурации ответа: тип контента, зерно генерации, соотношение сторон
     */
    @Builder.Default
    private final GenerationOptions generationOptions = GenerationOptions.builder().build();

    /**
     * Предпочтительные параметры конфигурации ответа: тип контента, зерно генерации, соотношение сторон
     */
    @Getter
    @Builder
    @ToString
    public static class GenerationOptions implements Serializable {

        /**
         * Тип графического контента выходного изображения.
         * По состоянию на 03.08.2024 поддерживается только {@code image/jpg}
         */
        @Builder.Default
        private final String mimeType = MediaType.IMAGE_JPEG_VALUE;

        /**
         * Зерно генерации изображения - любое число от 0 до 2<sup>64</sup>. Служит начальной точкой для
         * генерации изображения из шума и позволяет повторять результат при идентичном промпте и зерне
         */
        @Builder.Default
        private final long seed = new Random().nextLong(0, Long.MAX_VALUE);

        /** Соотношение сторон генерируемого изображения (опционально), ширина и высота */
        @Builder.Default
        private final AspectRatio aspectRatio = AspectRatio.builder().build();
    }

    @Getter
    @Builder
    @ToString
    public static class AspectRatio {

        @Builder.Default
        private final long widthRatio = 1L;

        @Builder.Default
        private final long heightRatio = 1L;

    }
}
