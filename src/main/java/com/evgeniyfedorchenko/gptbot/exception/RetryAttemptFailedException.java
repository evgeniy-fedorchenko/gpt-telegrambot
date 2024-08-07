package com.evgeniyfedorchenko.gptbot.exception;

public class RetryAttemptFailedException extends RuntimeException {

    public RetryAttemptFailedException(String message) {
        super(message);
    }

}
