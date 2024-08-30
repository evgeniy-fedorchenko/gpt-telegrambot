package com.efedorchenko.gptbot.yandex.service;

import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Scope("prototype")  // Сабмиттеры должны быть разные для каждого сервиса
public class RequestSubmitterAsync {
    private final ThreadPoolTaskExecutor executor;
    private final BlockingQueue<Runnable> requestQueue;

    public RequestSubmitterAsync(@Qualifier("requestSubmitterPoolExecutor") ThreadPoolTaskExecutor executor) {
        this.executor = executor;
        this.requestQueue = new LinkedBlockingQueue<>();;
    }

    public CompletableFuture<Response> submitRequest(Callable<Response> requestCallable) {
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();

        Runnable requestTask = () -> {
            try {
                responseFuture.complete(requestCallable.call()); // Устанавливаем результат в CompletableFuture
            } catch (Exception e) {
                responseFuture.completeExceptionally(e); // Устанавливаем исключение в CompletableFuture
            }
        };

        requestQueue.add(requestTask);
        return responseFuture;
    }

    private void doExecuteSubmitted() {

        /*
         * Бесконечно опрашиваем очередь блокирующим take()
         * Выполняем запрос
         * Ждем полсекунды
         * Снова идем за запросом
         */

    }

}


