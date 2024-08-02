package com.evgeniyfedorchenko.gptbot.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Configuration
@ConfigurationProperties(
        prefix = WebClientConfiguration.CONFIGURATION_PREFIX,
        ignoreUnknownFields = false,
        ignoreInvalidFields = false
)
public class WebClientConfiguration {

    static final String CONFIGURATION_PREFIX = "http-client";

    @Positive
    private int connectTimeout;
    @Positive
    private int responseTimeout;
    @Positive
    private int readTimeout;
    @Positive
    private int writeTimeout;
    @Positive
    private int responseMaxSize;


    @Bean
    public WebClient webClient() {

        HttpClient nettyHttpClient = HttpClient.create()

                .httpResponseDecoder(spec -> spec.maxHeaderSize(responseMaxSize))

                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );


        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(nettyHttpClient))
                .build();
    }
}
