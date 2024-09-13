package com.efedorchenko.gptbot.configuration.interceptor;

import com.efedorchenko.gptbot.aop.MdcConfigurer;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class RquidInterceptor implements Interceptor, Ordered {

    @Override
    public int getOrder() {
        return 1;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        String rquid = Optional.ofNullable(MDC.get(MdcConfigurer.RQUID))
                .orElseGet(() -> UUID.randomUUID().toString());

        Request request = chain.request();

        Request newRequest = request.newBuilder()
                .addHeader("x-request-id", rquid)
                .build();

        return chain.proceed(newRequest);
    }

}
