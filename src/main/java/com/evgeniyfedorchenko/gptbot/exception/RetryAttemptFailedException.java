package com.evgeniyfedorchenko.gptbot.exception;

import org.springframework.retry.support.RetryTemplate;

/**
 * Исключение бросается исключительно в том случае, когда {@link RetryTemplate} на
 * очередной попытке не смог получить удовлетворительный ответ от сервера
 */
public class RetryAttemptFailedException extends RuntimeException {

    public RetryAttemptFailedException(String message) {
        super(message);
    }

}
