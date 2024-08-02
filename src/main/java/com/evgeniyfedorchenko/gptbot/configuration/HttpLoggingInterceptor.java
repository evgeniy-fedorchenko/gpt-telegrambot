package com.evgeniyfedorchenko.gptbot.configuration;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Nonnull
    @Override
    public ClientHttpResponse intercept(@Nonnull HttpRequest request,
                                        @Nonnull byte[] body,
                                        @Nonnull ClientHttpRequestExecution execution) throws IOException {

        if (log.isTraceEnabled()) {
            log.trace("\nRequest Line: {}\nHeaders     : {}\nRequest body: {}",
                    request.getURI() + " " + request.getMethod(),
                    request.getHeaders(),
                    new String(body, StandardCharsets.UTF_8));
        }

        ClientHttpResponse response = execution.execute(request, body);

        if (log.isTraceEnabled()) {
            log.trace("\nStatus       : {}\nHeaders      : {}\nResponse body: {}",
                    response.getStatusCode() + " " + response.getStatusText(),
                    response.getHeaders(),
                    StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
        }
        return response;
    }

}
