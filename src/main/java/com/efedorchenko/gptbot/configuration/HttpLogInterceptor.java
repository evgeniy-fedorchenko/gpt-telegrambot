package com.efedorchenko.gptbot.configuration;

import com.efedorchenko.gptbot.utils.logging.DataFormatUtils;
import com.efedorchenko.gptbot.utils.logging.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLogInterceptor implements Interceptor {

    @Value("${logging.max-mess-length}")
    private int maxLength;
    private final LogUtils logUtils;

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
                    String contentType = request.header("Content-Type");
                    preparedBody = prepareBodyForLogging(buffer.readUtf8(), contentType);
                }
                String headers = request.headers().toString().replaceAll("\n", ", ");

                log.trace("\n{}Request line {}: {}\n{}Headers      {}: {}\n{}Request body {}: {}",
                        DataFormatUtils.BLUE, DataFormatUtils.RESET, request.method() + " " + request.url(),
                        DataFormatUtils.BLUE, DataFormatUtils.RESET, headers,
                        DataFormatUtils.BLUE, DataFormatUtils.RESET, preparedBody);

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
        String contentType = response.header("Content-Type");
        String bodyAsString = null;
        try {
            if (responseBody != null) {
                bodyAsString = responseBody.string();
            }
            String code = response.code() + " (" + (response.receivedResponseAtMillis() - response.sentRequestAtMillis()) + " ms)";
            String headers = response.headers().toString().replaceAll("\n", ", ");
            String preparedBody = prepareBodyForLogging(bodyAsString, contentType);

            log.trace("\n{}Status        {}: {}\n{}Headers       {}: {}\n{}Response body {}: {}",
                    DataFormatUtils.BLUE, DataFormatUtils.RESET, code,
                    DataFormatUtils.BLUE, DataFormatUtils.RESET, headers,
                    DataFormatUtils.BLUE, DataFormatUtils.RESET, preparedBody);

        } catch (IOException ex) {
            log.error("Cannot intercept response body. Maybe it's because of OutOfMemoryError. Ex: {}", ex.getMessage());
        }

        return responseBody != null && bodyAsString != null
                ? response.newBuilder()
                .body(ResponseBody.create(bodyAsString.getBytes(), responseBody.contentType()))
                .build()
                : response;
    }

    public @NotNull String prepareBodyForLogging(@Nullable String rawBody, String contentType) {
        if (rawBody == null) {
            return "no body";
        }
        if (contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return "<binary_data>";
        }
        String temp = DataFormatUtils.excludeBase64(rawBody.replaceAll("\n", ""));
        return logUtils.shortenString(temp, maxLength);

    }

}
