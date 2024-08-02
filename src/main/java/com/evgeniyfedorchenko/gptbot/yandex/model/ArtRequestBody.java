package com.evgeniyfedorchenko.gptbot.yandex.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Random;

import static com.evgeniyfedorchenko.gptbot.yandex.model.ArtRequestBody.AspectRatio.DEFAULT_HEIGHT_RATIO;
import static com.evgeniyfedorchenko.gptbot.yandex.model.ArtRequestBody.AspectRatio.DEFAULT_WIDTH_RATIO;


/**
 * Класс представляет собой тело запроса, который направляется на обработку модели {@code YandexART}
 */
@Getter
@Builder
public class ArtRequestBody {

    /**
     * Внутренний идентификатор модели {@code YandexART}, содержащий идентификатор каталога Yandex Cloud.
     * Параметр {@code <your-yandex-folder-id>} - это идентификатор каталога Yandex Cloud
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
    @Setter
    @Builder
    public static class GenerationOptions {

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

        /**
         *  Соотношение сторон генерируемого изображения (опционально), ширина и высота
         */
        @Builder.Default
        private final AspectRatio aspectRatio = new AspectRatio(DEFAULT_WIDTH_RATIO, DEFAULT_HEIGHT_RATIO);
    }

    public record AspectRatio(long widthRatio, long heightRatio) {
        static final long DEFAULT_WIDTH_RATIO = 1L;
        static final long DEFAULT_HEIGHT_RATIO = 1L;
    }
}

/*

{
    "modelUri": "art://a2gmcltgwefld9t36qwe/yandex-art/latest",
    "generationOptions": {
        "mimeType": "image/jpeg",
        "seed": "1863",
        "aspectRatio": {
            "widthRatio": "2",
            "heightRatio": "1"
        }
    },
    "messages": [
        {
            "weight": "1",
            "text": "узор из цветных пастельных суккулентов разных сортов, hd full wallpaper, четкий фокус, множество сложных деталей, глубина кадра, вид сверху"
        }
    ]
}

*/