package com.evgeniyfedorchenko.gptbot.configuration.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

// TODO 07.08.2024 11:32 - Реализовать HttpLoggingInterceptor. Тут будем логировать запросы и ответы по формату
//  2024-08-07 11:18:35.912 TRACE 497507 --- [sbpgate-1] [] com.evgeniyfedorchenko.gptbot.configuration.interceptor.HttpLoggingInterceptor:
//  Request line: POST full url
//  Headers     : [headers]
//  Request body: serialized request body
//  2024-08-07 11:18:36.197 TRACE 497507 --- [sbpgate-1] [] com.evgeniyfedorchenko.gptbot.configuration.interceptor.HttpLoggingInterceptor:
//  Status code  : 200 OK
//  Headers     : [headers]
//  Response body: serialized response body

@Slf4j
@Component
public class HttpLoggingInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return chain.proceed(chain.request());
    }
}
