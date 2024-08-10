package com.evgeniyfedorchenko.gptbot.configuration;

import com.evgeniyfedorchenko.gptbot.configuration.interceptor.HttpLogInterceptor;
import com.evgeniyfedorchenko.gptbot.configuration.properties.OkhttpProperties;
import lombok.AllArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@AllArgsConstructor
public class OkHttpClientConfiguration {

    public static final MediaType MT_APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");

    private final OkhttpProperties properties;
    private final HttpLogInterceptor httpLogInterceptor;

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

        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)

                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .retryOnConnectionFailure(false)

                .addInterceptor(httpLogInterceptor)

                .build();
    }
}
