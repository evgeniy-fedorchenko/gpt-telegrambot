package com.evgeniyfedorchenko.gptbot.configuration;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class HttpLogInterceptor implements Interceptor {

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

                String utf8 = "";
                if (request.body() != null) {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);

                    utf8 = buffer.readUtf8().replaceAll("\n", "");
                    utf8 = utf8.length() > 2000
                            ? utf8.substring(utf8.length() - 2000)
                            : utf8;
                }

                log.trace("\nRequest line: {}\nHeaders     : {}\nRequest body: {}",
                        request.method() + " " + request.url(),
                        request.headers().toString().replaceAll("\n", ", "),
                        utf8);

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
        ResponseBody respBody = response.body();
        String bodyStr = null;
        try {

            bodyStr = respBody != null
                    ? respBody.string().replaceAll("\n", "")
                    : "none";

            log.trace("\nStatus       : {}\nHeaders      : {}\nTime spent   : {}\nResponse body: {}",
                    response.code() + " " + response.message(),
                    response.headers().toString().replaceAll("\n", ", "),
                    response.receivedResponseAtMillis() - response.sentRequestAtMillis() + " ms",
                    bodyStr.length() > 2000 ? bodyStr.substring(bodyStr.length() - 2000) : bodyStr);

        } catch (IOException ex) {
            log.error("Cannot intercept response body. Maybe it's because of OutOfMemoryError. Ex: {}", ex.getMessage());
        }
        return respBody != null
                ? response.newBuilder()
                    .body(ResponseBody.create(bodyStr.getBytes(), respBody.contentType()))
                    .build()
                : response;
    }
}
