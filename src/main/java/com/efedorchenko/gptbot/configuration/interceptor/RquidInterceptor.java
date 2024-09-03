package com.efedorchenko.gptbot.configuration.interceptor;

import com.efedorchenko.gptbot.aop.MdcConfigurer;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Order(1)
@Component
public class RquidInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        String rquid = Optional.ofNullable(MDC.get(MdcConfigurer.RQUID))
                .orElseGet(() -> UUID.randomUUID().toString());

        Request request = chain.request();

        Request newRequest = request.newBuilder()
                .addHeader(MdcConfigurer.RQUID, rquid)
                .build();

        return chain.proceed(newRequest);
    }
}
