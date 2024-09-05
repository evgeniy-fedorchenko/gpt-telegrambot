package com.efedorchenko.gptbot.configuration.interceptor;

import com.efedorchenko.gptbot.utils.logging.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements Interceptor, Ordered {

    private final LogUtils logUtils;

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Метод для логирования http-запросов и получаемых ответов, включая их тела (если метод запроса POST)
     * Логирование происходит на уровне {@code TRACE} и включает в себя полный урл запроса, заголовки и тело
     * запроса, а так же код ответа, заголовки, тело и время потраченное на отправку запроса и получение ответа.
     *
     * @throws IOException если {@code chain.proceed(request)} выбрасывает соответствующее исключение
     */
    @NotNull
//    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        Request request = chain.request();

        if (log.isTraceEnabled()) {
            log.trace("\nRequest line  : {}\nHeaders       : {}\nRequest body  : {}",
                    request.method() + " " + request.url(), formatHeaders(request.headers()),
                    formatBody(extractBody(request), request.header(HttpHeaders.CONTENT_TYPE)));
        }

        Response response = chain.proceed(request);

        if (!log.isTraceEnabled()) {
            return response;
        }
        ResponseBody body = response.peekBody(Long.MAX_VALUE);
        String bodyString = body.string();

        log.trace("\nStatus        : {}\nHeaders       : {}\nResponse body : {}",
                response.code() + " (" + (response.receivedResponseAtMillis() - response.sentRequestAtMillis()) + " ms)",
                formatHeaders(response.headers()), formatBody(bodyString, response.header(HttpHeaders.CONTENT_TYPE)));
        return response.newBuilder()
                .body(ResponseBody.create(bodyString, body.contentType()))
                .build();
    }

    private String formatHeaders(Headers headers) {
        return "[" + headers.toMultimap().entrySet().stream()
                .map(e -> e.getKey() + ": \"" + String.join("\", \"", e.getValue()) + "\"")
                .collect(Collectors.joining(", ")) + "]";
    }

    private @NotNull String extractBody(Request request) {
        if (request.body() == null) {
            return "absent";
        }
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException ioe) {
            log.error("Filed to intercept body. Ex: ", ioe);
            return "unreadable";
        }
    }

    private @NotNull String formatBody(@Nullable String body, @Nullable String header) {
        if (body == null || body.isEmpty() || body.equals("absent")) {
            return "absent";
        }
        return Objects.equals(header, MediaType.APPLICATION_OCTET_STREAM_VALUE) ? "binary" : logUtils.format(body);
    }
}