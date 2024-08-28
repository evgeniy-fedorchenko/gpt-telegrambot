package com.efedorchenko.gptbot.yandex.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Модель представляет собой общий результат преобразование голосового сообщения. Одно, и только одно
 * из полей {@code recognizedMessage} или {@code errorMessage} не равно {@code null} и содержит
 * результат преобразования
 */
@Getter
@Builder
public class VoiceRecResult {

    /**
     * Результат успешного преобразования. Значение, если
     * оно присутствует, не является пустым или пробельным
     */
    private final String recognizedMessage;

    /**
     * Если это поле не равно {@code null}, значит во время преобразования
     * голосового сообщения в текст произошла ошибка. В таком случае, это
     * поле будет содержать соответствующий ответа телеграм-бота юзеру
     */
    private final String answerToErrorMessage;

}
