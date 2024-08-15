package com.efedorchenko.gptbot.yandex.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Класс, представляющий сгенерированный ответ сети {@code YandexGPT} на отправленный ей текстовый запрос
 * Этот объект представляет собой обертку над объектом {@link Result}, который содержит все данные по ответу
 */
@Getter
@ToString
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class GptAnswer implements Serializable {

    /** Целевой класс ответа, содержащий все необходимую информацию*/
    @NotNull
    private final Result result;

    /**
     * Целевой класс ответа модели, содержащий все информацию об ответе
     */
    @Getter
    @ToString
    @AllArgsConstructor(onConstructor_ = @JsonCreator)
    public static class Result implements Serializable {

        /** Список сгенерированных вариантов завершения */
        private final List<Alternative> alternatives;

        /** Набор статистики, описывающий количество токенов контента, использованных модель */
        private final Usage usage;

        /** Версия этой модели (меняется с каждым новым выпуском) */
        private final String modelVersion;

    }

    /**
     * Класс, представляющий один сгенерированный вариант завершения диалога
     */
    @Getter
    @ToString
    @AllArgsConstructor(onConstructor_ = @JsonCreator)
    public static class Alternative implements Serializable {

        /** Объект сообщения, представляющий собой оболочку выходных данных модель */
        private final GptMessageUnit message;

        /** Перечисление, представляющее статус генерации ответа */
        private final Status status;

    }

    /**
     * Набор статистики, описывающий количество токенов контента, использованных моделью
     */
    @Getter
    @ToString
    @AllArgsConstructor(onConstructor_ = @JsonCreator)
    public static class Usage implements Serializable {

        /** Количество токенов в текстовой части входных данных модели */
        private final String inputTextTokens;

        /** Общее количество токенов в сгенерированных завершениях */
        private final String completionTokens;

        /** Общее количество токенов, включая все входные токены и все сгенерированные токены */
        private final String totalTokens;

    }

    /**
     * Перечисление, представляющее статус, с которым завершилась генерация ответа
     */
    public enum Status {

        /**
         * Неуказанный статус генерации
         */
        ALTERNATIVE_STATUS_UNSPECIFIED,

        /**
         * Частично сгенерированный ответ
         */
        ALTERNATIVE_STATUS_PARTIAL,

        /**
         * Неполный окончательный ответ, возникший в результате
         * достижения максимально допустимого количества токенов
         */
        ALTERNATIVE_STATUS_TRUNCATED_FINAL,

        /**
         * Окончательный ответ сгенерирована без каких-либо ограничений
         */
        ALTERNATIVE_STATUS_FINAL,

        /**
         * Генерация ответа была остановлена из-за обнаружения потенциально конфиденциального содержимого в
         * запросе или сгенерированном ответе. Чтобы исправить это, измените приглашение и перезапустите генерацию
         */
        ALTERNATIVE_STATUS_CONTENT_FILTER;

        @Override
        public String toString() {
            return this.name();
        }
    }

}
