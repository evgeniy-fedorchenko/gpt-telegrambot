package com.evgeniyfedorchenko.gptbot.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {

    private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int RESPONSE_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    private static final int WRITE_TIMEOUT_MILLIS = 5_000;

    private static final int RESPONSE_MAX_BYTES_COUNT = 10_485_760;

    @Bean
    public WebClient webClient() {

        HttpClient nettyHttpClient = HttpClient.create()

                .httpResponseDecoder(spec -> spec.maxHeaderSize(RESPONSE_MAX_BYTES_COUNT))

                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(Duration.ofMillis(RESPONSE_TIMEOUT_MILLIS))
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                );


        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(nettyHttpClient))
                .build();
    }
}
