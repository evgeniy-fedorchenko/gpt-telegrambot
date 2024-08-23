package com.efedorchenko.gptbot.exception;

/**
 * Непроверяемое исключение для ошибок бизнес-логики приложения
 */
public class GptTelegramBotException extends RuntimeException {

    public GptTelegramBotException(String message) {
        super(message);
    }

    public GptTelegramBotException(String message, Throwable cause) {
        super(message, cause);
    }

    public GptTelegramBotException(Throwable cause) {
        super(cause);
    }
}
