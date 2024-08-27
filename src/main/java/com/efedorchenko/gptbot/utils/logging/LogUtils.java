package com.efedorchenko.gptbot.utils.logging;

import com.efedorchenko.gptbot.utils.DataFormatUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogUtils {


    public static final Marker LOGIC_MARKER = MarkerFactory.getMarker("LOGIC");
    public static final Marker POWER_MARKER = MarkerFactory.getMarker("POWER");
    public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker RANRE_MARKER = MarkerFactory.getMarker("RetryAttemptNotReadyException");
    public static final Marker FUTURE_CHECK = MarkerFactory.getMarker("CHECK");

    private final ObjectMapper objectMapper;

    private static final String PARAMS = "-> [{}]";
    private static final String RETURN = "<- [{}]";
    private static final String EX = "!- [{}]";

    public String shortenString(String args, Integer maxLength) {
        return args.length() > maxLength
                ? "..." + args.substring(args.length() - maxLength)
                : args;
    }

    @Async("executorServiceOfVirtual")
    public void doLogMethodAsyncForSomeoneElse(CompletableFuture<List<Object>> paramsForLogFuture,
                                               Object result,
                                               Logger logger,
                                               Log log,
                                               int maxLength) {

        BiConsumer<String, String> logFunction = logFunction(logger, log.level(), maxLength);

        if (result instanceof Throwable throwable) {
            logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
            logFunction.accept(EX, throwable.toString());

        } else if (result instanceof CompletableFuture<?> resultFuture) {
            resultFuture.whenComplete((resultComplete, exceptionComplete) -> {

                if (exceptionComplete != null) {
                    logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
                    logFunction.accept(EX, exceptionComplete.toString());

                } else {
                    logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
                    if (log.result()) {
                        logFunction.accept(RETURN, logPrepare(resultComplete));
                    }
                }
            });

        } else {
            logFunction.accept(PARAMS, logPrepare(paramsForLogFuture));
            if (log.result()) {
                logFunction.accept(RETURN, logPrepare(result));
            }
        }
    }

    private String logPrepare(Object object) {
        try {
            return DataFormatUtils.excludeBase64(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException ex) {
            log.error(LOGIC_MARKER, "Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return "Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage();
        }
    }

    private String logPrepare(CompletableFuture<List<Object>> runningFuture) {
        try {
            List<Object> objects = runningFuture.get();
            String[] strings = new String[objects.size()];

            for (int i = 0; i < objects.size(); i++) {
                strings[i] = DataFormatUtils.excludeBase64(objectMapper.writeValueAsString(objects.get(i)));
            }
            return String.join(", ", strings);

        } catch (InterruptedException | ExecutionException | JsonProcessingException ex) {
            log.error(LOGIC_MARKER, "Cannot log param as aspect. ParamsFuture are filed. Ex: {}", ex.getMessage());
            return "Cannot log param as aspect. ParamsFuture are filed. Ex: " + ex.getMessage();
        }
    }

    private BiConsumer<String, String> logFunction(Logger logger, Level level, int maxLength) {
        return switch (level) {
            case TRACE -> (pattern, args) -> logger.trace(pattern, shortenString(args, maxLength));
            case DEBUG -> (pattern, args) -> logger.debug(pattern, shortenString(args, maxLength));
            case WARN  -> (pattern, args) -> logger.warn(pattern, shortenString(args, maxLength));
            case ERROR -> (pattern, args) -> logger.error(pattern, shortenString(args, maxLength));
            default    -> (pattern, args) -> logger.info(pattern, shortenString(args, maxLength));
        };
    }
}
