package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.aop.MethodLogAspect;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class HttpLogInterceptor implements Interceptor {

    @Value("${logging.max-mess-length}")
    private int maxLength;

    /**
     * Метод для логирования http-запросов и получаемых ответов, включая их тела (если метод запроса POST)
     * Логирование происходит на уровне {@code TRACE} и включает в себя полный урл запроса, заголовки и тело
     * запроса, а так же код ответа, заголовки, тело и время потраченное на отправку запроса и получение ответа.
     *
     * @throws IOException если {@code chain.proceed(request)} выбрасывает соответствующее исключение
     */
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        final Request request = chain.request();

        if (log.isTraceEnabled()) {
            try {

                String preparedBody = "";
                if (request.body() != null) {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    preparedBody = prepareBodyForLogging(buffer.readUtf8());
                }
                String headers = request.headers().toString().replaceAll("\n", ", ");

                log.trace("\n{}Request line {}: {}\n{}Headers      {}: {}\n{}Request body {}: {}",
                        BLUE, RESET, request.method() + " " + request.url(),
                        BLUE, RESET, headers,
                        BLUE, RESET, preparedBody);

            } catch (IllegalArgumentException ex) {
                log.warn("Cannot intercept request. request.body() is not UTF-8 encoded. Ex: {}", ex.getMessage());
            } catch (IOException ex) {
                log.warn("Cannot intercept request. Something is wrong with the network. Ex: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Cannot intercept request. Unknown exception has occurred Ex: {}", ex.getMessage());
            }
        }

        Response response = chain.proceed(request);

        if (!log.isTraceEnabled()) {
            return response;
        }

        ResponseBody responseBody = response.body();
        String bodyAsString = null;
        try {
            if (responseBody != null) {
                bodyAsString = responseBody.string();
            }
            String code = response.code() + " (" + (response.receivedResponseAtMillis() - response.sentRequestAtMillis()) + " ms)";
            String headers = response.headers().toString().replaceAll("\n", ", ");
            String preparedBody = prepareBodyForLogging(bodyAsString);

            log.trace("\n{}Status        {}: {}\n{}Headers       {}: {}\n{}Response body {}: {}",
                    BLUE, RESET, code,
                    BLUE, RESET, headers,
                    BLUE, RESET, preparedBody);

        } catch (IOException ex) {
            log.error("Cannot intercept response body. Maybe it's because of OutOfMemoryError. Ex: {}", ex.getMessage());
        }

        return responseBody != null && bodyAsString != null
                ? response.newBuilder()
                .body(ResponseBody.create(bodyAsString.getBytes(), responseBody.contentType()))
                .build()
                : response;
    }

    private @NotNull String prepareBodyForLogging(@Nullable String rawBody) {
        if (rawBody == null) {
            return "no body";
        }
        String temp = MethodLogAspect.excludeBase64(rawBody.replaceAll("\n", ""));
        return temp.length() > maxLength
                ? "..." + temp.substring(temp.length() - maxLength)
                : temp;

    }

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

}
