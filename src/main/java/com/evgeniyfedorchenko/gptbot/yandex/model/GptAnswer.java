package com.evgeniyfedorchenko.gptbot.yandex.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Класс, представляющий сгенерированный ответ сети {@code YandexGPT} на отправленный ей текстовый запрос
 * Этот объект представляет собой обертку над объектом {@link Result}, который содержит все данные по ответу
 *
 * @param result целевой класс ответа, содержащий все необходимую информацию
 */
public record GptAnswer(@NotNull GptAnswer.Result result) {

    /**
     * Целевой класс ответа модели, содержащий все информацию об ответе
     *
     * @param alternatives Список сгенерированных вариантов завершения
     * @param usage        Набор статистики, описывающий количество токенов контента, использованных моделью
     * @param modelVersion Версия этой модели (меняется с каждым новым выпуском)
     */
    public record Result(List<Alternative> alternatives, Usage usage, String modelVersion) {
    }

    /**
     * Класс, представляющий один сгенерированный вариант завершения диалога
     *
     * @param message Объект сообщения, представляющий собой оболочку выходных данных модели
     * @param status  Перечисление, представляющее статус генерации ответа
     */
    public record Alternative(GptMessageUnit message, Status status) {
    }

    /**
     * Набор статистики, описывающий количество токенов контента, использованных моделью
     *
     * @param inputTextTokens  Количество токенов в текстовой части входных данных модели
     * @param completionTokens Общее количество токенов в сгенерированных завершениях.
     * @param totalTokens      Общее количество токенов, включая все входные токены и все сгенерированные токены.
     */
    public record Usage(int inputTextTokens, int completionTokens, int totalTokens) {
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
        ALTERNATIVE_STATUS_CONTENT_FILTER
    }
}

/*

{
    "result": {
        "alternatives": [
            {
                "message": {
                    "role": "assistant",
                    "text": "Альберт Эйнштейн — выдающийся физик, изучавший основы Вселенной, в частности, теорию относительности, термодинамику и электромагнетизм.\n\nИзвестные открытия учёного: специальная и общая теория относительности, квантовая теория фотоэффекта и др."
                },
                "status": "ALTERNATIVE_STATUS_FINAL"
            }
        ],
        "usage": {
            "inputTextTokens": "452",
            "completionTokens": "54",
            "totalTokens": "506"
        },
        "modelVersion": "18.01.2024"
    }
}

*/
