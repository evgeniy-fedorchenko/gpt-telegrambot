package com.evgeniyfedorchenko.gptbot.configuration.interceptor;

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

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        final Request request = chain.request();

        if (log.isTraceEnabled()) {

            Buffer buffer = new Buffer();
            if (request.body() != null) {
                request.body().writeTo(buffer);
            }
            log.trace("\nRequest line: {}\nHeaders     : {}\nRequest body: {}",
                    request.method() + " " + request.url(),
                    request.headers().toString().replaceAll("\n", ", "),
                    buffer.readUtf8().replaceAll("\n", ""));
        }

        Response response = chain.proceed(request);

        if (!log.isTraceEnabled()) {
            return response;
        }
        ResponseBody respBody = response.body();
        String bodyStr = respBody != null
                ? respBody.string().replaceAll("\n", "")
                : "none";

        log.trace("\nStatus       : {}\nHeaders      : {}\nTime spent   : {}\nResponse body: {}",
                response.code() + " " + response.message(),
                response.headers().toString().replaceAll("\n", ", "),
                response.receivedResponseAtMillis() - response.sentRequestAtMillis() + "ms",
                bodyStr.length() > 500 ? bodyStr.substring(bodyStr.length() - 500) : bodyStr);

        return respBody != null
                ? response.newBuilder()
                    .body(ResponseBody.create(bodyStr.getBytes(), respBody.contentType()))
                    .build()
                : response;
    }
}
