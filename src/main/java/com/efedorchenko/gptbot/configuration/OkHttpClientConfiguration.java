package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.configuration.properties.OkhttpProperties;
import lombok.AllArgsConstructor;
import okhttp3.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(OkhttpProperties.class)
public class OkHttpClientConfiguration {

    public static final MediaType MT_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");

    private final OkhttpProperties properties;

    private final List<Interceptor> okHttpInterceptors;

    @Bean
    public OkHttpClient okHttpClient() {

        ConnectionPool connectionPool = new ConnectionPool(
                properties.getMaxIdleConnections(),
                properties.getKeepAliveMillis(),
                TimeUnit.MILLISECONDS
        );

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(properties.getMaxParallelRequests());
        dispatcher.setMaxRequestsPerHost(properties.getMaxParallelRequestsPerHost());

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)

                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .retryOnConnectionFailure(false);

        okHttpInterceptors.stream()
                .sorted(Comparator.comparingInt(interceptor -> ((Ordered) interceptor).getOrder()))
                .forEach(builder::addInterceptor);

        return builder.build();
    }
}
