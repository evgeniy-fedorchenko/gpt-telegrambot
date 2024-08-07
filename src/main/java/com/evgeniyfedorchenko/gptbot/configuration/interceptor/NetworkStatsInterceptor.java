package com.evgeniyfedorchenko.gptbot.configuration.interceptor;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

// TODO 07.08.2024 11:31 - Реализовать NetworkStatsInterceptor. Тут будем собирать статистику по запросам в сеть,
//  объему трафика и тд, чтобы скорректировать настройки OkHttpClient
@Component
public class NetworkStatsInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        return chain.proceed(chain.request());
    }
}
