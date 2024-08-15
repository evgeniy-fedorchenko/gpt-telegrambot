package com.efedorchenko.gptbot.exception;

import org.springframework.retry.support.RetryTemplate;

/**
 * Исключение бросается исключительно в том случае, когда {@link RetryTemplate} на
 * очередной попытке не смог получить удовлетворительный ответ от сервера
 */
public class RetryAttemptNotReadyException extends RuntimeException {

    public RetryAttemptNotReadyException(String message) {
        super(message);
    }

}
