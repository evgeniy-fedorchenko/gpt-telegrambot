package com.evgeniyfedorchenko.gptbot.yandex.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Класс представляет один объект сообщения, отправляемого модели {@code YandexART} для генерации.
 * При множетстве описаний (объектов {@link ArtMessageUnit}) итоговое изображение будет в любом случае
 * только одно, рассчитанное из соотношения сторон
 */
@Getter
@Builder
public class ArtMessageUnit {

    /**
     * Вес текстового описания. Если в запросе присутствует больше одного описания, влияние каждого
     * описания будет рассчитываться на основе веса, при этом сумма всех весов будет равна 1
     */
    @Builder.Default
    private final double weight = 1D;

    /**
     * Текстовое описание изображения, т.н. "промпт", на основе которого будет происходить генерация
     */
    private final String text;

}
